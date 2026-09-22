package com.voltera.tarifaeventos.domain.model;

/**
 * Estados posibles de un cargo tarifario generado a partir de un consumo.
 * <ul>
 *   <li>SIN_CARGO: la lectura no supero el umbral (no hay consumo extra).</li>
 *   <li>LIQUIDADO: se calculo un cargo por consumo extra y quedo liquidado.</li>
 * </ul>
 */
public enum EstadoCargo {
    SIN_CARGO,
    LIQUIDADO
}
