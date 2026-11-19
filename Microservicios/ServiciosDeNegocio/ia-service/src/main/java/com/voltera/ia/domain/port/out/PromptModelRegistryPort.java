package com.voltera.ia.domain.port.out;

import java.time.Instant;
import java.util.List;

/**
 * Caja 6 · Prompt &amp; Model Registry. Las versiones de prompts y de modelos,
 * cada una con sus evaluaciones: qué versión está en producción, quién la aprobó
 * y cómo volver a la anterior. Un prompt es código: se versiona, se prueba y se
 * despliega, no se edita en caliente.
 */
public interface PromptModelRegistryPort {

    /** Versión registrada de una familia de prompt. */
    record VersionPrompt(String familia, String version, String estado, String descripcion,
                         String aprobadaPor, Instant aprobadaEn, String plantilla) {}

    /** Cambio de versión en producción (promoción o rollback). */
    record CambioVersion(Instant instante, String familia, String desde, String hacia,
                         String tipo, String responsable, String motivo) {}

    String PRODUCCION = "PRODUCCION";
    String CANDIDATA = "CANDIDATA";
    String RETIRADA = "RETIRADA";

    /** Versión del prompt activo para un agente (p. ej. "diagnosticador@v3"). */
    String versionPrompt(String agente);

    /** Plantilla del prompt activo para un agente (recuperada por versión). */
    String promptActivo(String agente);

    default List<String> familias() { return List.of(); }

    default List<VersionPrompt> versiones(String familia) { return List.of(); }

    /** Pone una versión en producción (la anterior pasa a RETIRADA). La compuerta de Evals la aplica el servicio. */
    default VersionPrompt promover(String familia, String version, String responsable, String motivo) {
        throw new UnsupportedOperationException("registro de solo lectura");
    }

    /** Vuelve a la versión que estaba en producción antes de la actual. */
    default VersionPrompt revertir(String familia, String responsable, String motivo) {
        throw new UnsupportedOperationException("registro de solo lectura");
    }

    default List<CambioVersion> historial() { return List.of(); }
}
