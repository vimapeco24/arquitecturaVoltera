package com.voltera.habilitacion.domain.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Fila de la tabla OUTBOX transaccional (patron Transactional Outbox, lamina 10).
 * El cambio de estado del Medidor y el evento a publicar se escriben juntos; un
 * relay publica al broker y marca la fila como publicada.
 */
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
