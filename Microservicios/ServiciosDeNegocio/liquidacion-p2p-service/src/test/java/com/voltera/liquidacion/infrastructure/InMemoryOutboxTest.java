package com.voltera.liquidacion.infrastructure;

import com.voltera.liquidacion.domain.model.MensajeOutbox;
import com.voltera.liquidacion.infrastructure.persistence.InMemoryOutbox;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("InMemoryOutbox")
class InMemoryOutboxTest {

    @Test
    @DisplayName("Agrega y lista pendientes en orden de creacion")
    void agregaPendientes() {
        InMemoryOutbox outbox = new InMemoryOutbox();
        MensajeOutbox a = outbox.agregar(MensajeOutbox.pendiente("T", "k1", "{}"));
        MensajeOutbox b = outbox.agregar(MensajeOutbox.pendiente("T", "k2", "{}"));
        assertEquals(2, outbox.pendientes().size());
        assertEquals(2, outbox.todos().size());
        assertFalse(a.publicado());
        assertNotNull(b.id());
    }

    @Test
    @DisplayName("marcarPublicado saca el mensaje de pendientes pero lo conserva en todos")
    void marcaPublicado() {
        InMemoryOutbox outbox = new InMemoryOutbox();
        MensajeOutbox a = outbox.agregar(MensajeOutbox.pendiente("T", "k1", "{}"));
        outbox.marcarPublicado(a.id());
        assertEquals(0, outbox.pendientes().size());
        assertEquals(1, outbox.todos().size());
        assertTrue(outbox.todos().get(0).publicado());
        assertNotNull(outbox.todos().get(0).publicadoEn());
    }

    @Test
    @DisplayName("marcarPublicado de un id inexistente no falla")
    void idInexistente() {
        InMemoryOutbox outbox = new InMemoryOutbox();
        assertDoesNotThrow(() -> outbox.marcarPublicado("nope"));
    }
}
