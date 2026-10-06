package com.voltera.telemetriacore.application;

import com.voltera.telemetriacore.domain.event.LecturaValidada;
import com.voltera.telemetriacore.domain.model.ConsumoAgregadoVista;
import com.voltera.telemetriacore.domain.model.ConsumoVista;
import com.voltera.telemetriacore.domain.model.Lectura;
import com.voltera.telemetriacore.domain.model.MensajeOutbox;
import com.voltera.telemetriacore.domain.model.SerieDeMedicion;
import com.voltera.telemetriacore.domain.port.in.RegistrarConsumoUseCase;
import com.voltera.telemetriacore.domain.port.out.ConsumoAgregadoRepositoryPort;
import com.voltera.telemetriacore.domain.port.out.ConsumoVistaRepositoryPort;
import com.voltera.telemetriacore.domain.port.out.OutboxPort;
import com.voltera.telemetriacore.domain.port.out.SerializadorEventosPort;
import com.voltera.telemetriacore.domain.port.out.SerieRepositoryPort;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * MS Telemetria Core (CQRS, lamina 02/03).
 *
 * <p><b>Command</b>: incorpora LecturaValidada a la {@link SerieDeMedicion} del
 * medidor (append-only, lado "TSDB"). Al cerrarse un {@code Intervalo15Min},
 * delega en el {@link ProyectorVista} (caja "Proyector · actualiza vistas") la
 * materializacion del lado QUERY (vista por intervalo + vista agregada) y emite
 * {@code ConsumoIntervaloRegistrado} por el OUTBOX. Un barrido periodico emite
 * {@code MedidorSinReporte} para medidores que dejaron de reportar.</p>
 */
public class TelemetriaCoreService implements RegistrarConsumoUseCase {

    private final SerieRepositoryPort series;
    private final ConsumoVistaRepositoryPort vistas;
    private final ConsumoAgregadoRepositoryPort vistasAgregadas;
    private final ProyectorVista proyector;
    private final OutboxPort outbox;
    private final SerializadorEventosPort serializador;
    private final int duracionMin;
    private final Duration ventanaSinReporte;

    public TelemetriaCoreService(SerieRepositoryPort series, ConsumoVistaRepositoryPort vistas,
                                 ConsumoAgregadoRepositoryPort vistasAgregadas, ProyectorVista proyector,
                                 OutboxPort outbox, SerializadorEventosPort serializador,
                                 int duracionMin, int sinReporteMin) {
        this.series = series;
        this.vistas = vistas;
        this.vistasAgregadas = vistasAgregadas;
        this.proyector = (proyector != null)
                ? proyector
                : new ProyectorVista(vistas, vistasAgregadas);
        this.outbox = outbox;
        this.serializador = serializador;
        this.duracionMin = duracionMin;
        this.ventanaSinReporte = Duration.ofMinutes(sinReporteMin);
    }

    /**
     * Sobrecarga de compatibilidad: construye el proyector y la vista agregada
     * internamente a partir del repositorio de vistas por intervalo. Usada por tests
     * y wirings que aun no inyectan el proyector explicito.
     */
    public TelemetriaCoreService(SerieRepositoryPort series, ConsumoVistaRepositoryPort vistas,
                                 OutboxPort outbox, SerializadorEventosPort serializador,
                                 int duracionMin, int sinReporteMin) {
        this(series, vistas,
                new com.voltera.telemetriacore.infrastructure.persistence.InMemoryConsumoAgregadoRepository(),
                null, outbox, serializador, duracionMin, sinReporteMin);
    }

    @Override
    public void registrarLectura(LecturaValidada lectura) {
        SerieDeMedicion serie = series.buscarPorMedidor(lectura.medidorSerial())
                .orElseGet(() -> new SerieDeMedicion(lectura.medidorSerial(), duracionMin));

        Optional<SerieDeMedicion.ConsumoNetoIntervalo> cerrado =
                serie.registrarLectura(new Lectura(lectura.consumoKwh(), lectura.capturadaEn()));

        series.guardar(serie);
        cerrado.ifPresent(this::proyectarYEmitir);
    }

    @Override
    public void crearSerie(String medidorSerial) {
        if (series.buscarPorMedidor(medidorSerial).isEmpty()) {
            series.guardar(new SerieDeMedicion(medidorSerial, duracionMin));
        }
    }

    @Override
    public int revisarSinReporte() {
        Instant ahora = Instant.now();
        int emitidos = 0;
        for (SerieDeMedicion serie : series.todas()) {
            if (serie.sinReporteDesde(ahora, ventanaSinReporte)) {
                encolar("MedidorSinReporte", serie.medidorSerial(), Map.of(
                        "medidorSerial", serie.medidorSerial(),
                        "ultimaLecturaEn", String.valueOf(serie.ultimaLecturaEn()),
                        "detectadoEn", ahora.toString()
                ));
                emitidos++;
            }
        }
        return emitidos;
    }

    private void proyectarYEmitir(SerieDeMedicion.ConsumoNetoIntervalo neto) {
        // Lado QUERY (CQRS): el Proyector materializa las vistas de lectura
        // (por intervalo + agregada). El command no toca directamente las vistas.
        proyector.proyectar(neto);
        // Evento de dominio: ConsumoIntervaloRegistrado.
        encolar("ConsumoIntervaloRegistrado", neto.medidorSerial(), Map.of(
                "medidorSerial", neto.medidorSerial(),
                "inicioIntervalo", neto.inicio().toString(),
                "finIntervalo", neto.fin().toString(),
                "consumoNetoKwh", String.valueOf(neto.consumoNetoKwh()),
                "numLecturas", String.valueOf(neto.numLecturas())
        ));
    }

    @Override
    public List<ConsumoVista> consumoPorMedidor(String medidorSerial) {
        return vistas.porMedidor(medidorSerial);
    }

    @Override
    public List<ConsumoVista> todas() {
        return vistas.todas();
    }

    @Override
    public Optional<ConsumoAgregadoVista> agregadoPorMedidor(String medidorSerial) {
        return vistasAgregadas.porMedidor(medidorSerial);
    }

    @Override
    public List<ConsumoAgregadoVista> agregados() {
        return vistasAgregadas.todas();
    }

    private void encolar(String tipoEvento, String clave, Map<String, String> payload) {
        outbox.agregar(MensajeOutbox.pendiente(tipoEvento, clave, serializador.aJson(payload)));
    }
}
