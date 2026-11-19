package com.voltera.liquidacion.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.voltera.liquidacion.domain.model.LiquidacionCalculada;
import com.voltera.liquidacion.domain.model.MensajeOutbox;
import com.voltera.liquidacion.infrastructure.messaging.JacksonSerializadorEventos;
import com.voltera.liquidacion.infrastructure.persistence.InMemoryOutbox;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("OutboxLiquidacionEventPublisher")
class OutboxLiquidacionEventPublisherTest {

    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private LiquidacionCalculada evento() {
        return new LiquidacionCalculada("evt-1", "TX-1", "V", "C", 10000L,
                BigDecimal.valueOf(460), BigDecimal.valueOf(4600), Instant.parse("2026-01-01T00:00:00Z"), "COM-1");
    }

    @Test
    @DisplayName("Registra el evento en el outbox con clave = transaccionId")
    void registraEnOutbox() throws Exception {
        InMemoryOutbox outbox = new InMemoryOutbox();
        var publisher = new OutboxLiquidacionEventPublisher(outbox, new JacksonSerializadorEventos(mapper));

        assertTrue(publisher.publicar(evento()));

        assertEquals(1, outbox.pendientes().size());
        MensajeOutbox m = outbox.pendientes().get(0);
        assertEquals("LiquidacionCalculada", m.tipoEvento());
        assertEquals("TX-1", m.clave());
        var json = mapper.readTree(m.payloadJson());
        assertEquals("evt-1", json.get("eventId").asText());
        assertEquals("TX-1", json.get("transaccionId").asText());
        assertEquals(10000L, json.get("energiaWh").asLong());
        assertEquals("COM-1", json.get("comunidadId").asText());
    }
}
