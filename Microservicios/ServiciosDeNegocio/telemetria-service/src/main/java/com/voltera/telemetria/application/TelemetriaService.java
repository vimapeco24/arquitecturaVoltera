package com.voltera.telemetria.application;

import com.voltera.telemetria.domain.exception.MedidorNoEncontradoException;
import com.voltera.telemetria.domain.model.ConsumoKwh;
import com.voltera.telemetria.domain.model.ConsumoRegistrado;
import com.voltera.telemetria.domain.model.LecturaConsumo;
import com.voltera.telemetria.domain.model.Medidor;
import com.voltera.telemetria.domain.model.MedidorId;
import com.voltera.telemetria.domain.port.in.ConsultarMedidorUseCase;
import com.voltera.telemetria.domain.port.in.DrenarBufferUseCase;
import com.voltera.telemetria.domain.port.in.GestionarMedidorUseCase;
import com.voltera.telemetria.domain.port.in.IngestarConsumoUseCase;
import com.voltera.telemetria.domain.port.out.BufferOfflinePort;
import com.voltera.telemetria.domain.port.out.EventPublisherPort;
import com.voltera.telemetria.domain.port.out.MedidorRepositoryPort;

import java.util.List;

/**
 * Servicio de aplicacion del bounded context Telemetria (emisor IoT).
 *
 * <p>Orquesta la ingesta de consumo de los prosumidores. Implementa el patron
 * <b>store-and-forward</b>: cuando el broker no esta disponible, el evento
 * {@link ConsumoRegistrado} se encola en el {@link BufferOfflinePort} y se
 * reenvia mas tarde al recuperar la conexion (metodo {@link #drenar()}), de modo
 * que ninguna lectura se pierde ("procesar sin conexion y luego desencolar").</p>
 */
public class TelemetriaService implements IngestarConsumoUseCase, GestionarMedidorUseCase,
        ConsultarMedidorUseCase, DrenarBufferUseCase {

    private final MedidorRepositoryPort repositorio;
    private final EventPublisherPort eventPublisher;
    private final BufferOfflinePort buffer;

    public TelemetriaService(MedidorRepositoryPort repositorio,
                             EventPublisherPort eventPublisher,
                             BufferOfflinePort buffer) {
        this.repositorio = repositorio;
        this.eventPublisher = eventPublisher;
        this.buffer = buffer;
    }

    // ---------------------------------------------------------------------
    // Ingesta de consumo (con store-and-forward)
    // ---------------------------------------------------------------------

    @Override
    public ResultadoIngesta ingestar(ComandoIngestarConsumo c) {
        Medidor medidor = obtener(c.medidorId());
        LecturaConsumo lectura = new LecturaConsumo(ConsumoKwh.de(c.consumoKwh()), c.capturadaEn());
        ConsumoRegistrado evento = medidor.registrarLectura(lectura);

        // Intento de publicacion directa al broker.
        boolean publicado = eventPublisher.publicar(evento);
        if (!publicado) {
            // Sin conexion: se guarda localmente y se reenviara al reconectar.
            buffer.encolar(evento);
            return new ResultadoIngesta(evento, false, true);
        }
        return new ResultadoIngesta(evento, true, false);
    }

    // ---------------------------------------------------------------------
    // Desencolado del buffer offline (store-and-forward -> forward)
    // ---------------------------------------------------------------------

    @Override
    public ResultadoDrenado drenar() {
        int reenviados = 0;
        for (ConsumoRegistrado evento : buffer.pendientes()) {
            boolean ok = eventPublisher.publicar(evento);
            if (ok) {
                buffer.confirmarEntrega(evento);
                reenviados++;
            } else {
                // El broker sigue caido: paramos y reintentaremos en el proximo ciclo.
                break;
            }
        }
        return new ResultadoDrenado(reenviados, buffer.tamano());
    }

    // ---------------------------------------------------------------------
    // Gestion del ciclo de vida del medidor
    // ---------------------------------------------------------------------

    @Override
    public Medidor registrar(ComandoRegistrarMedidor c) {
        Medidor medidor = Medidor.registrar(c.prosumidorId(), ConsumoKwh.de(c.umbralKwh()));
        return repositorio.guardar(medidor);
    }

    @Override
    public Medidor activar(String medidorId) {
        Medidor medidor = obtener(medidorId);
        medidor.activar();
        return repositorio.guardar(medidor);
    }

    @Override
    public Medidor suspender(String medidorId) {
        Medidor medidor = obtener(medidorId);
        medidor.suspender();
        return repositorio.guardar(medidor);
    }

    @Override
    public void eliminar(String medidorId) {
        Medidor medidor = obtener(medidorId);
        repositorio.eliminar(medidor.id());
    }

    // ---------------------------------------------------------------------
    // Consultas
    // ---------------------------------------------------------------------

    @Override
    public Medidor porId(String medidorId) {
        return obtener(medidorId);
    }

    @Override
    public List<Medidor> porProsumidor(String prosumidorId) {
        return repositorio.buscarPorProsumidor(prosumidorId);
    }

    @Override
    public List<Medidor> todos() {
        return repositorio.buscarTodos();
    }

    private Medidor obtener(String medidorId) {
        return repositorio.buscarPorId(MedidorId.de(medidorId))
                .orElseThrow(() -> new MedidorNoEncontradoException(medidorId));
    }
}
