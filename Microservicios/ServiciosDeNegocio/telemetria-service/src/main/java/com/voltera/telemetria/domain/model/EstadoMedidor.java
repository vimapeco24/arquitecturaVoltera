package com.voltera.telemetria.domain.model;

/**
 * Ciclo de vida de un medidor inteligente de un prosumidor.
 * INACTIVO -> ACTIVO -> SUSPENDIDO (y ACTIVO de nuevo).
 * Solo un medidor ACTIVO puede registrar lecturas de consumo.
 */
public enum EstadoMedidor {
    INACTIVO,
    ACTIVO,
    SUSPENDIDO
}
