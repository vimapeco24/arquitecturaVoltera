package com.voltera.telemetriacore.domain.model;

import java.time.Instant;
import java.util.UUID;

/** Fila OUTBOX transaccional (lamina 10). */
public record MensajeOutbox(
        String id,
        String tipoEvento,
        String clave,
        String payloadJson,
        Instant creadoEn,
        boolean publicado,
        Instant publicadoEn
) {
    public static MensajeOutbox pendiente(String tipoEvento, String clave, String payloadJson) {
        return new MensajeOutbox("obx-" + UUID.randomUUID(), tipoEvento, clave, payloadJson,
                Instant.now(), false, null);
    }

    public MensajeOutbox marcarPublicado() {
        return new MensajeOutbox(id, tipoEvento, clave, payloadJson, creadoEn, true, Instant.now());
    }
}
