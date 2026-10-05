package com.voltera.tarifaeventos.infrastructure.persistence;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("InMemoryInbox - tabla inbox de idempotencia por consumidor (lamina 10)")
class InMemoryInboxTest {

    @Test
    @DisplayName("Primera vez devuelve true; reentrega del mismo eventId devuelve false")
    void upsertDetectaDuplicado() {
        InMemoryInbox inbox = new InMemoryInbox();

        assertTrue(inbox.registrarSiEsNuevo("tarifa-eventos-consumer", "EVT-1", "ConsumoRegistrado"));
        assertFalse(inbox.registrarSiEsNuevo("tarifa-eventos-consumer", "EVT-1", "ConsumoRegistrado"));
        assertFalse(inbox.registrarSiEsNuevo("tarifa-eventos-consumer", "EVT-1", "ConsumoRegistrado"));

        assertEquals(1, inbox.totalUnicos());
        assertEquals(2, inbox.totalDuplicados());
    }

    @Test
    @DisplayName("El mismo eventId para consumidores distintos NO es duplicado (clave por consumidor)")
    void aislamientoPorConsumidor() {
        InMemoryInbox inbox = new InMemoryInbox();

        assertTrue(inbox.registrarSiEsNuevo("consumidor-A", "EVT-X", "ConsumoRegistrado"));
        assertTrue(inbox.registrarSiEsNuevo("consumidor-B", "EVT-X", "ConsumoRegistrado"));

        assertEquals(2, inbox.totalUnicos());
        assertEquals(0, inbox.totalDuplicados());
    }
}
