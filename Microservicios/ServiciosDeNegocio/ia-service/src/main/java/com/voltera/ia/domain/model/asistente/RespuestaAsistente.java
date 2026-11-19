package com.voltera.ia.domain.model.asistente;

import com.voltera.ia.domain.model.Evidencia;

import java.time.Instant;
import java.util.List;

/**
 * Respuesta del Assistant a una pregunta sobre un diagnóstico. Es informativa:
 * cita la evidencia del caso y, cuando aplica, presenta de forma NEUTRAL las
 * consecuencias de aprobar y de rechazar (guardrail de no persuasión). Nunca
 * recomienda firmar ni ofrece una acción: la firma es un control separado.
 *
 * @param diagnosticoId diagnóstico al que se refiere.
 * @param respuesta     el texto para el firmante (según su audiencia).
 * @param evidencias    fuentes citadas del caso (RAG + MCP).
 * @param promptVersion versión del prompt conversacional por audiencia (Tema 5).
 * @param noPersuasion  true: la respuesta pasó el guardrail de no persuasión.
 */
public record RespuestaAsistente(
        String diagnosticoId,
        String respuesta,
        List<Evidencia> evidencias,
        String promptVersion,
        boolean noPersuasion,
        Instant generadoEn,
        /** Intención detectada en la pregunta (CONSECUENCIAS, EVIDENCIA, TIEMPO, COSTO, PROCESO, SEGURIDAD, GENERAL). */
        String intencion,
        /** true si la pregunta no era sobre el diagnóstico (guardrail ENT-ALCANCE). */
        boolean fueraDeAlcance
) {
    public static RespuestaAsistente de(String diagnosticoId, String respuesta,
                                        List<Evidencia> evidencias, String promptVersion) {
        return de(diagnosticoId, respuesta, evidencias, promptVersion, "GENERAL", false);
    }

    public static RespuestaAsistente de(String diagnosticoId, String respuesta, List<Evidencia> evidencias,
                                        String promptVersion, String intencion, boolean fueraDeAlcance) {
        return new RespuestaAsistente(diagnosticoId, respuesta, List.copyOf(evidencias),
                promptVersion, true, Instant.now(), intencion, fueraDeAlcance);
    }
}
