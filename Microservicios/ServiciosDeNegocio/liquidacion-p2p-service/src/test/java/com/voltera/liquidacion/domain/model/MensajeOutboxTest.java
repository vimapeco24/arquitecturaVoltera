package com.voltera.liquidacion.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("MensajeOutbox")
class MensajeOutboxTest {

    @Test
    @DisplayName("pendiente crea un mensaje no publicado con id obx-*")
    void pendiente() {
        MensajeOutbox m = MensajeOutbox.pendiente("T", "k", "{}");
        assertTrue(m.id().startsWith("obx-"));
        assertFalse(m.publicado());
        assertNull(m.publicadoEn());
        assertNotNull(m.creadoEn());
    }

    @Test
    @DisplayName("marcarPublicado conserva datos y fija publicadoEn")
    void marcarPublicado() {
        MensajeOutbox m = MensajeOutbox.pendiente("T", "k", "{}");
        MensajeOutbox p = m.marcarPublicado();
        assertTrue(p.publicado());
        assertNotNull(p.publicadoEn());
        assertEquals(m.id(), p.id());
        assertEquals(m.clave(), p.clave());
    }
}
