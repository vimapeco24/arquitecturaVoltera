package com.voltera.ingesta.infrastructure.persistence;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("InMemoryInbox - idempotencia por consumidor (lamina 10)")
class InMemoryInboxTest {

    @Test
    @DisplayName("La primera observacion es nueva; las reentregas del mismo eventId son duplicados")
    void deduplicaPorEventId() {
        InMemoryInbox inbox = new InMemoryInbox();

        assertTrue(inbox.registrarSiEsNuevo("ingesta-consumer", "EVT-1", "LecturaValidada"),
                "la primera vez debe procesarse");
        assertFalse(inbox.registrarSiEsNuevo("ingesta-consumer", "EVT-1", "LecturaValidada"),
                "la reentrega del mismo eventId debe descartarse");
        assertFalse(inbox.registrarSiEsNuevo("ingesta-consumer", "EVT-1", "LecturaValidada"),
                "sigue siendo duplicado en reentregas posteriores");

        assertEquals(1, inbox.totalUnicos());
        assertEquals(2, inbox.totalDuplicados());
    }

    @Test
    @DisplayName("El mismo eventId en consumidores distintos NO colisiona (inbox por consumidor)")
    void aislaPorConsumidor() {
        InMemoryInbox inbox = new InMemoryInbox();

        assertTrue(inbox.registrarSiEsNuevo("consumidor-A", "EVT-9", "MedidorHabilitado"));
        assertTrue(inbox.registrarSiEsNuevo("consumidor-B", "EVT-9", "MedidorHabilitado"),
                "otro consumidor debe poder procesar el mismo eventId");
        assertEquals(2, inbox.totalUnicos());
        assertEquals(0, inbox.totalDuplicados());
    }
}
