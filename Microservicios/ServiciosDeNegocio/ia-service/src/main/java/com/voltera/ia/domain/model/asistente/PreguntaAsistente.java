package com.voltera.ia.domain.model.asistente;

import java.util.UUID;

/**
 * Pregunta de un firmante al Assistant sobre un diagnóstico concreto, antes de
 * firmar. El Assistant responde SOLO con ese diagnóstico, su propuesta y la
 * evidencia RAG; no tiene herramientas de acción.
 *
 * @param diagnosticoId diagnóstico sobre el que se conversa (acota el contexto).
 * @param audiencia     prosumidor (simple) u operador (técnico).
 * @param usuario       identidad del firmante (IAM).
 * @param texto         la pregunta en lenguaje natural.
 */
public record PreguntaAsistente(
        UUID diagnosticoId,
        Audiencia audiencia,
        String usuario,
        String texto
) {
}
