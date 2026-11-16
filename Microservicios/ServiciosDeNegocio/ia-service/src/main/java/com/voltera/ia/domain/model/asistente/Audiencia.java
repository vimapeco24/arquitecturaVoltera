package com.voltera.ia.domain.model.asistente;

/**
 * Audiencia del Assistant. El documento exige prompts distintos por audiencia:
 * el prosumidor recibe lenguaje simple; el operador, lenguaje técnico. La firma
 * siempre es un control separado de la conversación.
 */
public enum Audiencia {
    /** Dueño del activo, firma 3. Lenguaje simple, consecuencias claras. */
    PROSUMIDOR,
    /** Equipo de operaciones, firma 2. Lenguaje técnico. */
    OPERADOR
}
