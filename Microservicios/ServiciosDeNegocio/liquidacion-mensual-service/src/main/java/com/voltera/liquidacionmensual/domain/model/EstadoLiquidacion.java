package com.voltera.liquidacionmensual.domain.model;

/**
 * Ciclo de vida de una liquidacion mensual.
 */
public enum EstadoLiquidacion {
    ABIERTA,   // acepta nuevos movimientos
    CERRADA    // totalizada; base lista para facturar
}
