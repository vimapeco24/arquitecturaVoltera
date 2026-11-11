package com.voltera.ia.domain.model.der;

import java.time.Instant;
import java.util.UUID;

/**
 * Evento de dominio emitido al broker cuando el agente genera un diagnóstico:
 * topic.ia.diagnostico. Sin PII: solo activo, severidad, acción propuesta y si
 * exige aprobación humana (Audit & Retention).
 */
public record DiagnosticoDerGenerado(
        UUID eventId,
        UUID diagnosticoId,
        String activoId,
        String tipoActivo,
        String comunidadId,
        String severidad,
        String accionPropuesta,
        boolean requiereAprobacionHumana,
        String motor,
        Instant generadoEn,
        Instant emitidoEn
) {
    public static DiagnosticoDerGenerado desde(DiagnosticoDer d) {
        Instant ahora = Instant.now();
        return new DiagnosticoDerGenerado(
                UUID.randomUUID(), d.diagnosticoId(), d.activoId(), d.tipoActivo().name(),
                d.comunidadId(), d.severidad().name(), d.accionPropuesta().name(),
                d.requiereAprobacionHumana(), d.motor(), d.generadoEn(), ahora);
    }
}
