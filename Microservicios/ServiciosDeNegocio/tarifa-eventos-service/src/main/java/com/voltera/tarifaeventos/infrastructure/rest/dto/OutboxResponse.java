package com.voltera.tarifaeventos.infrastructure.rest.dto;

import com.voltera.tarifaeventos.domain.model.MensajeOutbox;

import java.time.Instant;

/**
 * Vista REST de una fila del outbox transaccional.
 */
public record OutboxResponse(
        String id,
        String tipoEvento,
        String clave,
        String payloadJson,
        Instant creadoEn,
        boolean publicado,
        Instant publicadoEn
) {
    public static OutboxResponse desde(MensajeOutbox m) {
        return new OutboxResponse(m.id(), m.tipoEvento(), m.clave(), m.payloadJson(),
                m.creadoEn(), m.publicado(), m.publicadoEn());
    }
}
