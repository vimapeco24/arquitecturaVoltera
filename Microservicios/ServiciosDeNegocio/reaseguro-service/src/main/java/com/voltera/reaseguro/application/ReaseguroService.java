package com.voltera.reaseguro.application;

import com.voltera.reaseguro.domain.event.SiniestroAprobado;
import com.voltera.reaseguro.domain.exception.CesionNoEncontradaException;
import com.voltera.reaseguro.domain.model.CesionRiesgo;
import com.voltera.reaseguro.domain.model.CesionVista;
import com.voltera.reaseguro.domain.model.MensajeInbox;
import com.voltera.reaseguro.domain.port.in.ConsultarCesionUseCase;
import com.voltera.reaseguro.domain.port.in.ProcesarEventoSiniestroUseCase;
import com.voltera.reaseguro.domain.port.out.CesionRepositoryPort;
import com.voltera.reaseguro.domain.port.out.CesionVistaRepositoryPort;
import com.voltera.reaseguro.domain.port.out.InboxPort;

import java.util.List;

/**
 * Servicio de aplicacion del bounded context Reaseguro.
 *
 * <p>Implementa CQRS: la escritura vive en {@link CesionRepositoryPort} y la
 * lectura se sirve desde la proyeccion {@link CesionVistaRepositoryPort}. El
 * procesamiento del evento es idempotente mediante una tabla INBOX por consumidor
 * ({@link InboxPort}): el upsert por ({@code consumidor}, {@code eventId}) descarta
 * las reentregas del broker (entrega al-menos-una-vez) sin duplicar la cesion.</p>
 */
public class ReaseguroService implements ProcesarEventoSiniestroUseCase, ConsultarCesionUseCase {

    /** Nombre logico del consumer group (clave de la inbox). */
    private static final String CONSUMIDOR = "reaseguro-consumer";

    private final CesionRepositoryPort escrituraRepo;
    private final CesionVistaRepositoryPort lecturaRepo;
    private final InboxPort inbox;

    public ReaseguroService(CesionRepositoryPort escrituraRepo,
                            CesionVistaRepositoryPort lecturaRepo,
                            InboxPort inbox) {
        this.escrituraRepo = escrituraRepo;
        this.lecturaRepo = lecturaRepo;
        this.inbox = inbox;
    }

    @Override
    public CesionRiesgo procesar(SiniestroAprobado evento) {
        // 1. IDEMPOTENCIA (tabla INBOX por consumidor): upsert por (consumidor, eventId).
        //    Si NO es nuevo => el broker reentrego el evento (al-menos-una-vez) y ya lo
        //    aplicamos: devolvemos la cesion existente sin volver a ceder.
        boolean esNuevo = inbox.registrarSiEsNuevo(CONSUMIDOR, evento.eventId(), "SiniestroAprobado");
        if (!esNuevo) {
            return escrituraRepo.buscarPorEventId(evento.eventId())
                    .orElseThrow(() -> new IllegalStateException(
                            "Evento marcado como duplicado en inbox pero sin cesion asociada: " + evento.eventId()));
        }

        // 2. Transferencia de estado: creamos la cesion con los datos del evento.
        CesionRiesgo cesion = CesionRiesgo.crearDesdeEvento(
                evento.eventId(),
                evento.siniestroId(),
                evento.polizaId(),
                evento.prosumidorId(),
                evento.montoAprobado()
        );

        // 3. Simulacion de aprobacion por la reaseguradora: PENDIENTE -> CEDIDA.
        cesion.confirmarCesion();

        // 4. Guardamos en el modelo de ESCRITURA.
        escrituraRepo.guardar(cesion);

        // 5. Proyectamos a la vista de LECTURA (CQRS).
        lecturaRepo.guardar(CesionVista.desdeEscritura(cesion));

        return cesion;
    }

    @Override
    public CesionVista porId(String id) {
        return lecturaRepo.buscarPorId(id)
                .orElseThrow(() -> new CesionNoEncontradaException(
                        "No se encontro la cesion con id: " + id));
    }

    @Override
    public List<CesionVista> porPoliza(String polizaId) {
        return lecturaRepo.buscarPorPoliza(polizaId);
    }

    @Override
    public List<CesionVista> todas() {
        return lecturaRepo.buscarTodas();
    }

    @Override
    public List<MensajeInbox> inbox() {
        return inbox.todos();
    }

    @Override
    public long inboxUnicos() {
        return inbox.totalUnicos();
    }

    @Override
    public long inboxDuplicados() {
        return inbox.totalDuplicados();
    }
}
