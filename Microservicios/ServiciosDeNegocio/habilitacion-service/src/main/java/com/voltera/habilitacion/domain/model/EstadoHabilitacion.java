package com.voltera.habilitacion.domain.model;

/**
 * Estado de habilitacion del medidor (BC Habilitacion de Medidores, lamina 02).
 */
public enum EstadoHabilitacion {
    /** El alta arranco: se emitio MedidorHabilitado y se esperan CanalIngestaCreado + TarifaAsignada. */
    PENDIENTE,
    /** Llegaron ambas confirmaciones: el medidor quedo activado (MedidorActivado). */
    ACTIVADO,
    /** Vencio el plazo sin ambas confirmaciones: HabilitacionFallida + MedidorSuspendido. */
    SUSPENDIDO
}
