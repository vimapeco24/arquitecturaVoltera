package com.voltera.pagos.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.voltera.pagos.domain.model.MensajeOutbox;
import com.voltera.pagos.domain.model.Monto;
import com.voltera.pagos.domain.model.OrdenPago;
import com.voltera.pagos.domain.model.TipoPago;
import com.voltera.pagos.infrastructure.messaging.JacksonSerializadorEventos;
import com.voltera.pagos.infrastructure.persistence.InMemoryOutbox;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("OutboxEventosPago - PagoConfirmado hacia Cobros")
class OutboxEventosPagoTest {

    private InMemoryOutbox outbox;
    private OutboxEventosPago eventos;
    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        outbox = new InMemoryOutbox();
        eventos = new OutboxEventosPago(outbox, new JacksonSerializadorEventos(mapper));
    }

    @Test
    @DisplayName("Un COBRO_FACTURA registra PagoConfirmado con el formato que consume Cobros")
    void registraPagoConfirmado() throws Exception {
        eventos.pagoConfirmado(OrdenPago.crear("PRO-1", "FAC-100", TipoPago.COBRO_FACTURA, Monto.de(47600)));

        List<MensajeOutbox> pendientes = outbox.pendientes();
        assertEquals(1, pendientes.size());
        MensajeOutbox m = pendientes.get(0);
        assertEquals("PagoConfirmado", m.tipoEvento());
        assertEquals("FAC-100", m.clave());
        JsonNode j = mapper.readTree(m.payloadJson());
        assertTrue(j.get("eventId").asText().startsWith("evt-"));
        assertEquals("FAC-100", j.get("referencia").asText());
        assertEquals("PRO-1", j.get("clienteId").asText());
        assertEquals("FAC-100", j.get("polizaId").asText());
        assertEquals(0, j.get("montoPagado").decimalValue().compareTo(java.math.BigDecimal.valueOf(47600)));
        assertDoesNotThrow(() -> Instant.parse(j.get("confirmadoEn").asText()));
        assertDoesNotThrow(() -> Instant.parse(j.get("emitidoEn").asText()));
    }

    @Test
    @DisplayName("Un PAGO_EXCEDENTE no genera evento")
    void ignoraPagoExcedente() {
        eventos.pagoConfirmado(OrdenPago.crear("PRO-1", "EXC-1", TipoPago.PAGO_EXCEDENTE, Monto.de(100)));
        assertTrue(outbox.todos().isEmpty());
    }

    @Test
    @DisplayName("Cada evento recibe un eventId distinto")
    void eventIdUnico() throws Exception {
        var orden = OrdenPago.crear("PRO-1", "FAC-1", TipoPago.COBRO_FACTURA, Monto.de(10));
        eventos.pagoConfirmado(orden);
        eventos.pagoConfirmado(orden);
        var ids = outbox.todos().stream().map(m -> {
            try { return mapper.readTree(m.payloadJson()).get("eventId").asText(); }
            catch (Exception e) { throw new RuntimeException(e); }
        }).distinct().count();
        assertEquals(2, ids);
    }
}
