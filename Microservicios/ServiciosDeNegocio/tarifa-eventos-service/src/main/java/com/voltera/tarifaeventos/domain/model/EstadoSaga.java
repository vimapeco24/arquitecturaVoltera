package com.voltera.tarifaeventos.domain.model;

/**
 * Estados de la saga de alta de medidor (process manager, lamina 10).
 */
public enum EstadoSaga {
    /** Alta iniciada; se publico MedidorHabilitado y se esperan los colaboradores. */
    INICIADA,
    /** Llegaron CanalIngestaCreado y TarifaAsignada: alta consistente. */
    COMPLETADA,
    /** Vencieron 15 min sin ambas respuestas: compensada (HabilitacionFallida + MedidorSuspendido). */
    FALLIDA
}
