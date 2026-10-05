package com.voltera.tarifaeventos.application;

import com.voltera.tarifaeventos.domain.event.ConsumoRegistrado;
import com.voltera.tarifaeventos.domain.exception.CargoNoEncontradoException;
import com.voltera.tarifaeventos.domain.model.CargoTarifa;
import com.voltera.tarifaeventos.domain.model.CargoVista;
import com.voltera.tarifaeventos.domain.model.NotificacionLiquidada;
import com.voltera.tarifaeventos.domain.port.in.ActivarMedidorUseCase;
import com.voltera.tarifaeventos.domain.port.in.ConsultarCargoUseCase;
import com.voltera.tarifaeventos.domain.port.in.ProcesarConsumoUseCase;
import com.voltera.tarifaeventos.domain.port.out.CargoRepositoryPort;
import com.voltera.tarifaeventos.domain.port.out.CargoVistaRepositoryPort;
import com.voltera.tarifaeventos.domain.port.out.InboxPort;
import com.voltera.tarifaeventos.domain.port.out.NotificacionPublisherPort;

import java.util.List;

/**
 * Servicio de aplicacion del bounded context Tarifa-Eventos (consumidor).
 *
 * <p>Consume el evento {@code ConsumoRegistrado} del medidor del prosumidor y, si
 * hubo consumo extra, calcula un cargo tarifario. Implementa:</p>
 * <ul>
 *   <li><b>CQRS</b>: escritura en {@link CargoRepositoryPort}, lectura desde la
 *       proyeccion {@link CargoVistaRepositoryPort} (Data-per-service).</li>
 *   <li><b>Idempotencia</b> por {@code eventId} (el broker entrega al-menos-una-vez).</li>
 *   <li><b>Transferencia de estado</b>: usa los datos del evento sin consultar al origen.</li>
 *   <li><b>Notificacion liquidada</b>: al activar un medidor publica la factura/tarifas
 *       en el topico de notificacion.</li>
 * </ul>
 */
public class TarifaEventosService implements ProcesarConsumoUseCase, ConsultarCargoUseCase,
        ActivarMedidorUseCase {

    private final CargoRepositoryPort escrituraRepo;
    private final CargoVistaRepositoryPort lecturaRepo;
    private final NotificacionPublisherPort notificacionPublisher;
    private final InboxPort inbox;

    /** Nombre logico del consumer group (clave de la tabla inbox, lamina 10). */
    public static final String CONSUMIDOR = "tarifa-eventos-consumer";

    public TarifaEventosService(CargoRepositoryPort escrituraRepo,
                                CargoVistaRepositoryPort lecturaRepo,
                                NotificacionPublisherPort notificacionPublisher,
                                InboxPort inbox) {
        this.escrituraRepo = escrituraRepo;
        this.lecturaRepo = lecturaRepo;
        this.notificacionPublisher = notificacionPublisher;
        this.inbox = inbox;
    }

    // ---------------------------------------------------------------------
    // Consumo de eventos (CQRS + idempotencia + transferencia de estado)
    // ---------------------------------------------------------------------

    @Override
    public CargoTarifa procesar(ConsumoRegistrado evento) {
        // 1. IDEMPOTENCIA (tabla INBOX por consumidor, lamina 10):
        //    upsert por (consumidor, eventId). Si NO es nuevo => el broker reentrego
        //    el evento (al-menos-una-vez) y ya lo aplicamos: devolvemos el cargo
        //    existente sin volver a cobrar.
        boolean esNuevo = inbox.registrarSiEsNuevo(CONSUMIDOR, evento.eventId(), "ConsumoRegistrado");
        if (!esNuevo) {
            return escrituraRepo.buscarPorEventId(evento.eventId())
                    .orElseThrow(() -> new IllegalStateException(
                            "Evento marcado como duplicado en inbox pero sin cargo asociado: " + evento.eventId()));
        }

        // 2. Transferencia de estado: creamos el cargo con los datos del evento.
        CargoTarifa cargo = CargoTarifa.crearDesdeEvento(
                evento.eventId(),
                evento.medidorId(),
                evento.prosumidorId(),
                evento.consumoKwh(),
                evento.umbralKwh(),
                evento.consumoExtra()
        );

        // 3. Guardamos en el modelo de ESCRITURA.
        escrituraRepo.guardar(cargo);

        // 4. Proyectamos a la vista de LECTURA (CQRS).
        lecturaRepo.guardar(CargoVista.desdeEscritura(cargo));

        return cargo;
    }

    // ---------------------------------------------------------------------
    // Activacion de medidor -> notificacion liquidada
    // ---------------------------------------------------------------------

    @Override
    public NotificacionLiquidada activar(ComandoActivarMedidor c) {
        NotificacionLiquidada notificacion = NotificacionLiquidada.porActivacion(
                c.medidorId(), c.prosumidorId(), c.umbralKwh());
        notificacionPublisher.publicar(notificacion);
        return notificacion;
    }

    // ---------------------------------------------------------------------
    // Consultas (solo lado LECTURA)
    // ---------------------------------------------------------------------

    @Override
    public CargoVista porId(String id) {
        return lecturaRepo.buscarPorId(id)
                .orElseThrow(() -> new CargoNoEncontradoException("No se encontro el cargo con id: " + id));
    }

    @Override
    public List<CargoVista> porProsumidor(String prosumidorId) {
        return lecturaRepo.buscarPorProsumidor(prosumidorId);
    }

    @Override
    public List<CargoVista> todos() {
        return lecturaRepo.buscarTodas();
    }
}
