package com.voltera.telemetriacore.domain.model;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/**
 * Agregado raiz del BC <b>Telemetria · Core</b> (CQRS, lamina 02). Hay <b>una serie
 * por medidor</b>.
 *
 * <p>Lado <b>command</b>: incorpora {@code LecturaValidada} de forma append-only y
 * acumula el consumo del {@link Intervalo15Min} en curso. Cuando llega una lectura
 * de un intervalo posterior, <b>cierra</b> el intervalo anterior y produce un
 * {@link ConsumoNetoIntervalo} que el servicio publica como
 * {@code ConsumoIntervaloRegistrado} (y el proyector materializa en el lado query).</p>
 *
 * <p>Si el medidor no reporta dentro de la ventana configurada, el servicio emite
 * {@code MedidorSinReporte} usando {@link #ultimaLecturaEn()}.</p>
 */
public class SerieDeMedicion {

    private final String medidorSerial;
    private final int duracionMin;

    private Intervalo15Min intervaloEnCurso;
    private double acumuladoKwh;
    private long lecturasEnIntervalo;
    private Instant ultimaLecturaEn;

    public SerieDeMedicion(String medidorSerial, int duracionMin) {
        if (medidorSerial == null || medidorSerial.isBlank()) {
            throw new IllegalArgumentException("medidorSerial es obligatorio");
        }
        this.medidorSerial = medidorSerial;
        this.duracionMin = duracionMin;
    }

    /**
     * Rehidrata la serie desde un snapshot persistido (p. ej. adaptador TSDB/JDBC).
     * Reconstruye el estado del intervalo en curso sin reproducir lectura a lectura.
     */
    public static SerieDeMedicion rehidratar(String medidorSerial, int duracionMin,
                                             Instant intervaloEnCursoInicio, double acumuladoKwh,
                                             long lecturasEnIntervalo, Instant ultimaLecturaEn) {
        SerieDeMedicion s = new SerieDeMedicion(medidorSerial, duracionMin);
        s.intervaloEnCurso = (intervaloEnCursoInicio != null)
                ? new Intervalo15Min(intervaloEnCursoInicio, duracionMin)
                : null;
        s.acumuladoKwh = acumuladoKwh;
        s.lecturasEnIntervalo = lecturasEnIntervalo;
        s.ultimaLecturaEn = ultimaLecturaEn;
        return s;
    }

    /**
     * Incorpora una lectura validada. Si pertenece a un intervalo posterior al
     * actual, cierra el actual y devuelve su {@link ConsumoNetoIntervalo}.
     *
     * @return el consumo neto del intervalo cerrado, si lo hubo.
     */
    public Optional<ConsumoNetoIntervalo> registrarLectura(Lectura lectura) {
        this.ultimaLecturaEn = lectura.capturadaEn();
        Intervalo15Min intervaloLectura = Intervalo15Min.de(lectura.capturadaEn(), duracionMin);

        Optional<ConsumoNetoIntervalo> cerrado = Optional.empty();
        if (intervaloEnCurso == null) {
            intervaloEnCurso = intervaloLectura;
        } else if (!intervaloLectura.inicio().equals(intervaloEnCurso.inicio())) {
            // Llego una lectura de otro intervalo: cerramos el anterior.
            cerrado = Optional.of(cerrarIntervalo());
            intervaloEnCurso = intervaloLectura;
        }
        acumuladoKwh += lectura.consumoKwh();
        lecturasEnIntervalo++;
        return cerrado;
    }

    /** Cierra manualmente el intervalo en curso (p. ej. al vencer la ventana). */
    public Optional<ConsumoNetoIntervalo> cerrarIntervaloEnCurso() {
        if (intervaloEnCurso == null || lecturasEnIntervalo == 0) {
            return Optional.empty();
        }
        return Optional.of(cerrarIntervalo());
    }

    private ConsumoNetoIntervalo cerrarIntervalo() {
        ConsumoNetoIntervalo neto = new ConsumoNetoIntervalo(
                medidorSerial, intervaloEnCurso.inicio(), intervaloEnCurso.fin(),
                acumuladoKwh, lecturasEnIntervalo);
        acumuladoKwh = 0;
        lecturasEnIntervalo = 0;
        return neto;
    }

    public boolean sinReporteDesde(Instant ahora, Duration ventana) {
        if (ultimaLecturaEn == null) return false;
        return Duration.between(ultimaLecturaEn, ahora).compareTo(ventana) >= 0;
    }

    public String medidorSerial() { return medidorSerial; }
    public Instant ultimaLecturaEn() { return ultimaLecturaEn; }
    public double acumuladoKwh() { return acumuladoKwh; }

    // Getters de snapshot para la persistencia (adaptador TSDB/JDBC).
    public int duracionMin() { return duracionMin; }
    public long lecturasEnIntervalo() { return lecturasEnIntervalo; }
    public Instant intervaloEnCursoInicio() {
        return intervaloEnCurso != null ? intervaloEnCurso.inicio() : null;
    }

    /** Consumo neto resultante de cerrar un intervalo de 15 min. */
    public record ConsumoNetoIntervalo(String medidorSerial, Instant inicio, Instant fin,
                                       double consumoNetoKwh, long numLecturas) {}
}
