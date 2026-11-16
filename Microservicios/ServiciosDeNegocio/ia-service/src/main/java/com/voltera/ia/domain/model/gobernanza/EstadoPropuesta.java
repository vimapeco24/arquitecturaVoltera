package com.voltera.ia.domain.model.gobernanza;

/**
 * Estados del ciclo de vida de una propuesta de acción en el contrato
 * {@code MultiFirmaAccionDER}. La acción solo se ejecuta desde APROBADA (3/3);
 * cualquier rechazo la deja RECHAZADA y el vencimiento del plazo la deja VENCIDA.
 * Ninguna transición ejecuta el comando sin las tres firmas (fail-closed).
 */
public enum EstadoPropuesta {
    /** Registrada por el Agente de Acción, aún sin las 3 firmas. */
    PENDIENTE,
    /** Las 3 firmas (IA + operador + prosumidor) están registradas. */
    APROBADA,
    /** Un firmante rechazó; queda constancia on-chain con la razón. */
    RECHAZADA,
    /** Venció el plazo sin completar las 3 firmas; no se ejecuta. */
    VENCIDA,
    /** El comando aprobado fue ejecutado y su resultado quedó registrado. */
    EJECUTADA
}
