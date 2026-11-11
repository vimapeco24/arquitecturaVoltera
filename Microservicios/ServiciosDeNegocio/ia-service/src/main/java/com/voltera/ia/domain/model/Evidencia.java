package com.voltera.ia.domain.model;

/**
 * Fragmento de evidencia recuperado por RAG o por una herramienta (microservicio
 * / cadena), con su fuente citable. Sin evidencia, el agente "alucina"; por eso
 * cada recomendacion lleva la lista de fuentes que la sustentan.
 *
 * @param fuente  de donde viene (p. ej. "RAG:polizas", "tool:siniestros-service",
 *                "chain:blockchain-gateway").
 * @param titulo  etiqueta corta de la evidencia.
 * @param detalle contenido citable (ya enmascarado si traia PII).
 */
public record Evidencia(String fuente, String titulo, String detalle) {

    public static Evidencia de(String fuente, String titulo, String detalle) {
        return new Evidencia(fuente, titulo, detalle);
    }
}
