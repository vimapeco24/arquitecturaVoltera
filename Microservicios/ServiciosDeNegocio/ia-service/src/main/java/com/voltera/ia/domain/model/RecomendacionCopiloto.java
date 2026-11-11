package com.voltera.ia.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Resultado del Copiloto de Siniestros. Es una PROPUESTA, no una decision:
 * incluye la accion sugerida, su justificacion, la evidencia citada (RAG +
 * herramientas + cadena), la traza del flujo agentico y si exige revision humana.
 *
 * <p>La banda de Responsible AI vive aqui: {@code requiereRevisionHumana} y el
 * motor que la produjo ({@code motor}: "llm" o "local") quedan registrados para
 * auditoria.</p>
 */
public record RecomendacionCopiloto(
        UUID recomendacionId,
        String siniestroId,
        Decision decisionPropuesta,
        BigDecimal montoSugerido,
        NivelConfianza confianza,
        boolean requiereRevisionHumana,
        String justificacion,
        List<Evidencia> evidencias,
        List<PasoFlujo> traza,
        String motor,
        boolean datosEnmascarados,
        Instant generadaEn
) {
    public static RecomendacionCopiloto crear(
            String siniestroId,
            Decision decision,
            BigDecimal montoSugerido,
            NivelConfianza confianza,
            boolean requiereRevisionHumana,
            String justificacion,
            List<Evidencia> evidencias,
            List<PasoFlujo> traza,
            String motor,
            boolean datosEnmascarados) {
        return new RecomendacionCopiloto(
                UUID.randomUUID(),
                siniestroId,
                decision,
                montoSugerido,
                confianza,
                requiereRevisionHumana,
                justificacion,
                List.copyOf(evidencias),
                List.copyOf(traza),
                motor,
                datosEnmascarados,
                Instant.now());
    }
}
