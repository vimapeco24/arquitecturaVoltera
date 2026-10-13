package com.voltera.cobros.domain.model;

import java.time.Instant;

/**
 * Registro de la <b>tabla INBOX</b> de idempotencia por consumidor (BC Cobros).
 *
 * <p>El broker entrega al-menos-una-vez; un upsert por ({@code consumidor},
 * {@code eventId}) garantiza que un {@code PagoConfirmado} reentregado no concilie
 * ni emita el recibo dos veces.</p>
 */
public record MensajeInbox(
        String consumidor,
        String eventId,
        String tipoEvento,
        Instant primeraVez,
        long vecesVistas
) {
    public static MensajeInbox nuevo(String consumidor, String eventId, String tipoEvento) {
        return new MensajeInbox(consumidor, eventId, tipoEvento, Instant.now(), 1L);
    }

    public MensajeInbox vistoDeNuevo() {
        return new MensajeInbox(consumidor, eventId, tipoEvento, primeraVez, vecesVistas + 1);
    }

    public boolean esDuplicado() {
        return vecesVistas > 1;
    }
}
