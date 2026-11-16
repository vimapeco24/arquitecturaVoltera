package com.voltera.ia.domain.model.gobernanza;

/**
 * Los tres firmantes del contrato multi-firma {@code MultiFirmaAccionDER} (3/3).
 * El orden refleja el ciclo de firmas del documento: el agente certifica que la
 * recomendación proviene del sistema, el operador valida técnicamente y el
 * prosumidor consiente la intervención sobre su propio activo.
 *
 * <p>El gestor de comunidad NO es firmante: cada DER está a cargo de un único
 * prosumidor. No existe modo degradado 2/3: la acción solo se ejecuta con las 3.</p>
 */
public enum Firmante {
    /** Firma 1 · el Agente de Acción firma la propuesta que él mismo generó. */
    AGENTE_IA,
    /** Firma 2 · el operador de Voltera valida el diagnóstico técnicamente. */
    OPERADOR,
    /** Firma 3 · el prosumidor, dueño del activo, consiente la intervención. */
    PROSUMIDOR
}
