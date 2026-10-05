package com.voltera.tarifaeventos.application;

import com.voltera.tarifaeventos.domain.exception.SagaNoEncontradaException;
import com.voltera.tarifaeventos.domain.model.MensajeOutbox;
import com.voltera.tarifaeventos.domain.model.SagaAltaMedidor;
import com.voltera.tarifaeventos.domain.port.in.OrquestarAltaMedidorUseCase;
import com.voltera.tarifaeventos.domain.port.out.OutboxPort;
import com.voltera.tarifaeventos.domain.port.out.SagaRepositoryPort;
import com.voltera.tarifaeventos.domain.port.out.SerializadorEventosPort;

import java.time.Instant;
import java.util.Map;

/**
 * Process manager (orquestacion/mediador) del alta de medidor — lamina 10.
 *
 * <p>Coordina el alta publicando (via OUTBOX transaccional) {@code MedidorHabilitado}
 * y esperando {@code CanalIngestaCreado} + {@code TarifaAsignada}. Si vencen 15 min
 * sin ambas respuestas, compensa emitiendo {@code HabilitacionFallida} y
 * {@code MedidorSuspendido}.</p>
 *
 * <p><b>Outbox transaccional:</b> cada metodo guarda primero el estado de la saga y a
 * continuacion encola el/los eventos en el outbox. En un adaptador relacional ambos
 * writes irian en la MISMA transaccion local; un relay publica el outbox al broker.
 * Asi no hay doble escritura (estado sin evento, o evento sin estado).</p>
 */
public class ProcessManagerAltaMedidor implements OrquestarAltaMedidorUseCase {

    private final SagaRepositoryPort sagas;
    private final OutboxPort outbox;
    private final SerializadorEventosPort serializador;

    public ProcessManagerAltaMedidor(SagaRepositoryPort sagas,
                                     OutboxPort outbox,
                                     SerializadorEventosPort serializador) {
        this.sagas = sagas;
        this.outbox = outbox;
        this.serializador = serializador;
    }

    @Override
    public SagaAltaMedidor iniciarAlta(String medidorId, String prosumidorId) {
        SagaAltaMedidor saga = SagaAltaMedidor.iniciar(medidorId, prosumidorId);
        sagas.guardar(saga);
        encolar("MedidorHabilitado", saga.medidorId(), Map.of(
                "sagaId", saga.sagaId(),
                "medidorId", saga.medidorId(),
                "prosumidorId", String.valueOf(saga.prosumidorId()),
                "iniciadaEn", saga.iniciadaEn().toString()
        ));
        return saga;
    }

    @Override
    public SagaAltaMedidor registrarCanalIngestaCreado(String sagaId) {
        SagaAltaMedidor saga = obtener(sagaId);
        saga.registrarCanalIngestaCreado();
        sagas.guardar(saga);
        emitirCompletadaSiAplica(saga);
        return saga;
    }

    @Override
    public SagaAltaMedidor registrarTarifaAsignada(String sagaId) {
        SagaAltaMedidor saga = obtener(sagaId);
        saga.registrarTarifaAsignada();
        sagas.guardar(saga);
        emitirCompletadaSiAplica(saga);
        return saga;
    }

    @Override
    public int procesarTimeouts() {
        Instant ahora = Instant.now();
        int compensadas = 0;
        for (SagaAltaMedidor saga : sagas.pendientes()) {
            if (saga.marcarFallidaPorTimeout(ahora)) {
                sagas.guardar(saga);
                emitirCompensaciones(saga);
                compensadas++;
            }
        }
        return compensadas;
    }

    @Override
    public SagaAltaMedidor forzarTimeout(String sagaId) {
        SagaAltaMedidor saga = obtener(sagaId);
        if (saga.forzarTimeout()) {
            sagas.guardar(saga);
            emitirCompensaciones(saga);
        }
        return saga;
    }

    /** Compensacion del alta fallida: HabilitacionFallida + MedidorSuspendido (outbox). */
    private void emitirCompensaciones(SagaAltaMedidor saga) {
        encolar("HabilitacionFallida", saga.medidorId(), Map.of(
                "sagaId", saga.sagaId(),
                "medidorId", saga.medidorId(),
                "motivo", String.valueOf(saga.motivoFallo())
        ));
        encolar("MedidorSuspendido", saga.medidorId(), Map.of(
                "sagaId", saga.sagaId(),
                "medidorId", saga.medidorId(),
                "motivo", "Alta no confirmada en 15 min"
        ));
    }

    private void emitirCompletadaSiAplica(SagaAltaMedidor saga) {
        if (saga.estado() == com.voltera.tarifaeventos.domain.model.EstadoSaga.COMPLETADA) {
            encolar("MedidorAltaCompletada", saga.medidorId(), Map.of(
                    "sagaId", saga.sagaId(),
                    "medidorId", saga.medidorId(),
                    "finalizadaEn", String.valueOf(saga.finalizadaEn())
            ));
        }
    }

    private void encolar(String tipoEvento, String clave, Map<String, String> payload) {
        outbox.agregar(MensajeOutbox.pendiente(tipoEvento, clave, serializador.aJson(payload)));
    }

    private SagaAltaMedidor obtener(String sagaId) {
        return sagas.buscarPorId(sagaId)
                .orElseThrow(() -> new SagaNoEncontradaException(sagaId));
    }
}
