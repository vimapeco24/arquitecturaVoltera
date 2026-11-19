package com.voltera.pagos.infrastructure;

import com.voltera.pagos.domain.model.MensajeOutbox;
import com.voltera.pagos.infrastructure.persistence.InMemoryOutbox;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("InMemoryOutbox y MensajeOutbox")
class InMemoryOutboxTest {

    @Test
    @DisplayName("MensajeOutbox nace pendiente y marcarPublicado fija la fecha")
    void mensaje() {
        MensajeOutbox m = MensajeOutbox.pendiente("PagoConfirmado", "k", "{}");
        assertTrue(m.id().startsWith("obx-"));
        assertFalse(m.publicado());
        assertNull(m.publicadoEn());
        MensajeOutbox p = m.marcarPublicado();
        assertTrue(p.publicado());
        assertNotNull(p.publicadoEn());
        assertEquals(m.id(), p.id());
    }

    @Test
    @DisplayName("Agrega, lista pendientes y excluye los publicados")
    void flujo() {
        InMemoryOutbox outbox = new InMemoryOutbox();
        MensajeOutbox a = outbox.agregar(MensajeOutbox.pendiente("T", "k1", "{}"));
        MensajeOutbox b = outbox.agregar(MensajeOutbox.pendiente("T", "k2", "{}"));
        assertEquals(2, outbox.pendientes().size());
        outbox.marcarPublicado(a.id());
        assertEquals(1, outbox.pendientes().size());
        assertEquals(b.id(), outbox.pendientes().get(0).id());
        assertEquals(2, outbox.todos().size());
    }

    @Test
    @DisplayName("Marcar un id inexistente no falla")
    void idInexistente() {
        InMemoryOutbox outbox = new InMemoryOutbox();
        assertDoesNotThrow(() -> outbox.marcarPublicado("nada"));
        assertTrue(outbox.todos().isEmpty());
    }
}
