package com.voltera.ia.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Evento de dominio emitido al broker cuando el copiloto genera una
 * recomendacion: topic.ia.recomendacion. Otros servicios (observabilidad,
 * notificaciones, un futuro panel de Human Review) pueden suscribirse.
 *
 * <p>No incluye la descripcion libre ni PII: solo la decision propuesta, el
 * monto, la confianza y si exige revision humana (Zero-retention / Audit).</p>
 */
public record IaRecomendacionGenerada(
        UUID eventId,
        UUID recomendacionId,
        String siniestroId,
        String decisionPropuesta,
        BigDecimal montoSugerido,
        String confianza,
        boolean requiereRevisionHumana,
        String motor,
        Instant generadaEn,
        Instant emitidoEn
) {
    public static IaRecomendacionGenerada desde(RecomendacionCopiloto r) {
        Instant ahora = Instant.now();
        return new IaRecomendacionGenerada(
                UUID.randomUUID(),
                r.recomendacionId(),
                r.siniestroId(),
                r.decisionPropuesta().name(),
                r.montoSugerido(),
                r.confianza().name(),
                r.requiereRevisionHumana(),
                r.motor(),
                r.generadaEn(),
                ahora);
    }
}
