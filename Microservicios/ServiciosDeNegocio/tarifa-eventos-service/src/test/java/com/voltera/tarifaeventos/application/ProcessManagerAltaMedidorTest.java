package com.voltera.tarifaeventos.application;

import com.voltera.tarifaeventos.domain.model.EstadoSaga;
import com.voltera.tarifaeventos.domain.model.MensajeOutbox;
import com.voltera.tarifaeventos.domain.model.SagaAltaMedidor;
import com.voltera.tarifaeventos.domain.port.out.SerializadorEventosPort;
import com.voltera.tarifaeventos.infrastructure.persistence.InMemoryOutbox;
import com.voltera.tarifaeventos.infrastructure.persistence.InMemorySagaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ProcessManagerAltaMedidor - orquestacion, outbox y timeout (lamina 10)")
class ProcessManagerAltaMedidorTest {

    private InMemorySagaRepository sagas;
    private InMemoryOutbox outbox;
    private ProcessManagerAltaMedidor pm;

    /** Serializador trivial para test (no depende de Jackson). */
    static class FakeSerializador implements SerializadorEventosPort {
        @Override public String aJson(Object payload) { return String.valueOf(payload); }
    }

    @BeforeEach
    void setUp() {
        sagas = new InMemorySagaRepository();
        outbox = new InMemoryOutbox();
        pm = new ProcessManagerAltaMedidor(sagas, outbox, new FakeSerializador());
    }

    private List<String> tiposOutbox() {
        return outbox.todos().stream().map(MensajeOutbox::tipoEvento).toList();
    }

    @Test
    @DisplayName("Iniciar alta: estado INICIADA y MedidorHabilitado encolado en el outbox")
    void iniciarAltaEmiteMedidorHabilitado() {
        SagaAltaMedidor saga = pm.iniciarAlta("MED-1", "PRO-1");

        assertEquals(EstadoSaga.INICIADA, saga.estado());
        assertTrue(tiposOutbox().contains("MedidorHabilitado"));
        assertEquals(1, outbox.pendientes().size());
    }

    @Test
    @DisplayName("Camino feliz: CanalIngestaCreado + TarifaAsignada -> COMPLETADA y MedidorAltaCompletada en outbox")
    void caminoFelizCompletaSaga() {
        SagaAltaMedidor saga = pm.iniciarAlta("MED-2", "PRO-2");

        pm.registrarCanalIngestaCreado(saga.sagaId());
        SagaAltaMedidor despues = pm.registrarTarifaAsignada(saga.sagaId());

        assertEquals(EstadoSaga.COMPLETADA, despues.estado());
        assertTrue(despues.canalIngestaCreado());
        assertTrue(despues.tarifaAsignada());
        assertTrue(tiposOutbox().contains("MedidorAltaCompletada"));
        // No se emitieron compensaciones.
        assertFalse(tiposOutbox().contains("HabilitacionFallida"));
        assertFalse(tiposOutbox().contains("MedidorSuspendido"));
    }

    @Test
    @DisplayName("Timeout forzado: solo CanalIngestaCreado -> FALLIDA + HabilitacionFallida + MedidorSuspendido")
    void timeoutCompensaConHabilitacionFallidaYSuspension() {
        SagaAltaMedidor saga = pm.iniciarAlta("MED-3", "PRO-3");
        pm.registrarCanalIngestaCreado(saga.sagaId()); // falta TarifaAsignada

        SagaAltaMedidor fallida = pm.forzarTimeout(saga.sagaId());

        assertEquals(EstadoSaga.FALLIDA, fallida.estado());
        assertNotNull(fallida.motivoFallo());
        assertTrue(fallida.motivoFallo().contains("TarifaAsignada"));
        assertTrue(tiposOutbox().contains("HabilitacionFallida"));
        assertTrue(tiposOutbox().contains("MedidorSuspendido"));
    }

    @Test
    @DisplayName("procesarTimeouts no compensa sagas recien iniciadas (dentro de la ventana de 15 min)")
    void procesarTimeoutsNoCompensaDentroDeLaVentana() {
        pm.iniciarAlta("MED-4", "PRO-4");
        int compensadas = pm.procesarTimeouts();
        assertEquals(0, compensadas);
    }

    @Test
    @DisplayName("Las respuestas tardias tras COMPLETADA no cambian el estado (idempotencia de la saga)")
    void respuestasTardiasNoAlteranSagaCompletada() {
        SagaAltaMedidor saga = pm.iniciarAlta("MED-5", "PRO-5");
        pm.registrarCanalIngestaCreado(saga.sagaId());
        pm.registrarTarifaAsignada(saga.sagaId());

        SagaAltaMedidor otraVez = pm.registrarCanalIngestaCreado(saga.sagaId());
        assertEquals(EstadoSaga.COMPLETADA, otraVez.estado());
    }
}
