package com.voltera.ia.domain.port.out;

/**
 * Caja 6 · Prompt &amp; Model Registry. Las versiones de prompts y de modelos,
 * cada una con sus evaluaciones: qué versión está en producción, quién la aprobó
 * y cómo volver a la anterior. Un prompt es código: se versiona, se prueba y se
 * despliega, no se edita en caliente.
 */
public interface PromptModelRegistryPort {

    /** Versión del prompt activo para un agente (p. ej. "diagnosticador@v3"). */
    String versionPrompt(String agente);

    /** Plantilla del prompt activo para un agente (recuperada por versión). */
    String promptActivo(String agente);
}
