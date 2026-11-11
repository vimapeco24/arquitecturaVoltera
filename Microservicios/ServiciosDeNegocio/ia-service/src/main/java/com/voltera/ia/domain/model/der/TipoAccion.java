package com.voltera.ia.domain.model.der;

/**
 * Acción correctiva que PROPONE el Agente-Acción. Las acciones físicas sobre el
 * activo o el despacho de un técnico comprometen la continuidad del servicio:
 * NUNCA las decide el LLM, siempre pasan por Human Review (human-in-the-loop).
 */
public enum TipoAccion {
    /** Solo observar / recolectar más telemetría. No compromete nada. */
    MONITOREAR,
    /** Crear orden de trabajo para despachar un técnico a campo. */
    DESPACHAR_TECNICO,
    /** Acción física sobre el activo (reiniciar inversor, descargar batería...). */
    COMANDO_FISICO,
    /** Cortar/limitar suministro del activo. Máximo impacto. */
    CORTAR_SUMINISTRO
}
