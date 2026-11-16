package com.voltera.ia.domain.model.gobernanza;

import java.time.Instant;
import java.util.UUID;

/**
 * Evento de dominio del ciclo de gobernanza, publicado al tópico
 * {@code der.gobernanza}. Corresponde a los eventos del documento:
 * PropuestaAccionCreada · FirmaRegistrada · FirmaVencida · AccionAprobada ·
 * AccionRechazada · AccionEjecutada. Sin PII: solo identificadores, estado,
 * conteo de firmas y la referencia on-chain (txRef).
 */
public record EventoGobernanza(
        UUID eventId,
        String tipo,
        UUID propuestaId,
        UUID diagnosticoId,
        String activoId,
        String estado,
        String firmas,
        String txRef,
        Instant emitidoEn
) {
    public static EventoGobernanza de(String tipo, PropuestaAccion p, String txRef) {
        return new EventoGobernanza(
                UUID.randomUUID(), tipo, p.propuestaId(), p.diagnosticoId(), p.activoId(),
                p.estado().name(), p.firmasResumen(), txRef, Instant.now());
    }
}
