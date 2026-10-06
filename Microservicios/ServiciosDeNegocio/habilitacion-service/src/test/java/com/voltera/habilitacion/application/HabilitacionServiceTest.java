package com.voltera.habilitacion.application;

import com.voltera.habilitacion.domain.event.OrdenInstalacionCerrada;
import com.voltera.habilitacion.domain.model.EstadoHabilitacion;
import com.voltera.habilitacion.domain.model.Medidor;
import com.voltera.habilitacion.domain.model.MensajeOutbox;
import com.voltera.habilitacion.domain.port.out.SerializadorEventosPort;
import com.voltera.habilitacion.infrastructure.persistence.InMemoryMedidorRepository;
import com.voltera.habilitacion.infrastructure.persistence.InMemoryOutbox;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("HabilitacionService - mediador del alta (lamina 02)")
class HabilitacionServiceTest {

    private InMemoryMedidorRepository repo;
    private InMemoryOutbox outbox;
    private HabilitacionService service;

    static class FakeSerializador implements SerializadorEventosPort {
        @Override public String aJson(Object payload) { return String.valueOf(payload); }
    }

    @BeforeEach
    void setUp() {
        repo = new InMemoryMedidorRepository();
        outbox = new InMemoryOutbox();
        service = new HabilitacionService(repo, outbox, new FakeSerializador());
    }

    private OrdenInstalacionCerrada orden(String medidorId) {
        return new OrdenInstalacionCerrada("EVT-" + medidorId, "ORD-1", medidorId,
                "SER-1", "Landis", "PM-1", "Calle 1", Instant.now());
    }

    private List<String> tiposOutbox() {
        return outbox.todos().stream().map(MensajeOutbox::tipoEvento).toList();
    }

    @Test
    @DisplayName("OrdenInstalacionCerrada -> Medidor PENDIENTE y MedidorHabilitado en outbox")
    void habilitarEmiteMedidorHabilitado() {
        Medidor m = service.habilitarDesdeOrden(orden("MED-1"));
        assertEquals(EstadoHabilitacion.PENDIENTE, m.estado());
        assertTrue(tiposOutbox().contains("MedidorHabilitado"));
    }

    @Test
    @DisplayName("CanalIngestaCreado + TarifaAsignada -> ACTIVADO y MedidorActivado en outbox")
    void caminoFelizActiva() {
        service.habilitarDesdeOrden(orden("MED-2"));
        service.registrarCanalIngestaCreado("MED-2");
        Medidor m = service.registrarTarifaAsignada("MED-2");

        assertEquals(EstadoHabilitacion.ACTIVADO, m.estado());
        assertTrue(tiposOutbox().contains("MedidorActivado"));
        assertFalse(tiposOutbox().contains("MedidorSuspendido"));
    }

    @Test
    @DisplayName("Timeout forzado -> SUSPENDIDO + HabilitacionFallida + MedidorSuspendido")
    void timeoutSuspende() {
        service.habilitarDesdeOrden(orden("MED-3"));
        service.registrarCanalIngestaCreado("MED-3"); // falta TarifaAsignada
        Medidor m = service.forzarTimeout("MED-3");

        assertEquals(EstadoHabilitacion.SUSPENDIDO, m.estado());
        assertTrue(m.motivoFallo().contains("TarifaAsignada"));
        assertTrue(tiposOutbox().contains("HabilitacionFallida"));
        assertTrue(tiposOutbox().contains("MedidorSuspendido"));
    }

    @Test
    @DisplayName("procesarTimeouts no suspende dentro de la ventana de 15 min")
    void noSuspendeDentroDeVentana() {
        service.habilitarDesdeOrden(orden("MED-4"));
        assertEquals(0, service.procesarTimeouts());
    }
}
