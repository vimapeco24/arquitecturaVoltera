package com.voltera.cobros.application;

import com.voltera.cobros.domain.event.PagoConfirmado;
import com.voltera.cobros.domain.model.EstadoMovimiento;
import com.voltera.cobros.domain.model.Movimiento;
import com.voltera.cobros.domain.model.Prima;
import com.voltera.cobros.domain.port.in.RegistrarMovimientoUseCase.ComandoRegistrarMovimiento;
import com.voltera.cobros.infrastructure.persistence.InMemoryInbox;
import com.voltera.cobros.infrastructure.persistence.InMemoryMovimientoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class CobrosServiceTest {

    private InMemoryMovimientoRepository repo;
    private InMemoryInbox inbox;
    private CobrosService service;

    @BeforeEach
    void setUp() {
        repo = new InMemoryMovimientoRepository();
        inbox = new InMemoryInbox();
        service = new CobrosService(repo, inbox);
    }

    private PagoConfirmado pago(String eventId, String poliza, BigDecimal monto) {
        Instant ahora = Instant.now();
        return new PagoConfirmado(eventId, "REF-" + eventId, "CLI-1", poliza, monto, ahora, ahora);
    }

    @Test
    @DisplayName("registrar crea un movimiento PENDIENTE con su prima")
    void registraMovimientoPendiente() {
        Movimiento m = service.registrar(new ComandoRegistrarMovimiento(
                "CLI-1", "POL-1", Prima.de(new BigDecimal("50000"), Prima.Periodicidad.MENSUAL)));

        assertEquals(EstadoMovimiento.PENDIENTE, m.estado());
        assertEquals(0, new BigDecimal("50000").compareTo(m.prima().monto()));
        assertNull(m.recibo());
    }

    @Test
    @DisplayName("PagoConfirmado concilia el movimiento, emite recibo y pasa a CONCILIADO")
    void pagoConcilia() {
        service.registrar(new ComandoRegistrarMovimiento(
                "CLI-1", "POL-9", Prima.de(new BigDecimal("30000"), Prima.Periodicidad.MENSUAL)));

        Movimiento m = service.procesar(pago("EVT-1", "POL-9", new BigDecimal("30000")));

        assertEquals(EstadoMovimiento.CONCILIADO, m.estado());
        assertNotNull(m.recibo());
        assertEquals(0, new BigDecimal("30000").compareTo(m.recibo().montoPagado()));
    }

    @Test
    @DisplayName("idempotencia: reenviar el mismo eventId no concilia dos veces (0 duplicados de efecto)")
    void idempotenciaNoDuplica() {
        service.registrar(new ComandoRegistrarMovimiento(
                "CLI-1", "POL-7", Prima.de(new BigDecimal("20000"), Prima.Periodicidad.MENSUAL)));

        Movimiento primera = service.procesar(pago("EVT-DUP", "POL-7", new BigDecimal("20000")));
        Movimiento segunda = service.procesar(pago("EVT-DUP", "POL-7", new BigDecimal("20000")));

        assertEquals(primera.id().valor(), segunda.id().valor());
        assertEquals(EstadoMovimiento.CONCILIADO, segunda.estado());
        assertEquals(1, service.inboxUnicos());
        assertEquals(1, service.inboxDuplicados());
        // Un solo movimiento conciliado para la póliza.
        assertEquals(1, service.porPoliza("POL-7").size());
    }

    @Test
    @DisplayName("transferencia de estado: si no hay movimiento previo, el pago crea y concilia con los datos del evento")
    void transferenciaDeEstadoCreaYConcilia() {
        Movimiento m = service.procesar(pago("EVT-2", "POL-NUEVA", new BigDecimal("15000")));

        assertEquals(EstadoMovimiento.CONCILIADO, m.estado());
        assertEquals("POL-NUEVA", m.polizaId());
        assertEquals(0, new BigDecimal("15000").compareTo(m.recibo().montoPagado()));
    }
}
