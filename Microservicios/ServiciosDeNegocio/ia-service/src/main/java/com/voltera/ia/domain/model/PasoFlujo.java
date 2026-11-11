package com.voltera.ia.domain.model;

import java.time.Instant;

/**
 * Un paso del flujo agentico acotado (grafo tipo LangGraph). Cada paso deja una
 * traza observable y auditable: que se hizo, con que patron, y el resultado.
 *
 * @param orden      posicion en el flujo (1..N).
 * @param nodo       nombre del nodo del grafo (p. ej. "disparo", "rag", "react",
 *                   "tool-calling", "cadena", "human-in-the-loop", "registro").
 * @param patron     patron de arquitectura reutilizado (RAG, ReAct, Tool calling,
 *                   Cadena como verdad, Responsible AI, Observabilidad).
 * @param detalle    descripcion legible de lo ocurrido en el paso.
 * @param ejecutadoEn marca de tiempo.
 */
public record PasoFlujo(int orden, String nodo, String patron, String detalle, Instant ejecutadoEn) {

    public static PasoFlujo de(int orden, String nodo, String patron, String detalle) {
        return new PasoFlujo(orden, nodo, patron, detalle, Instant.now());
    }
}
