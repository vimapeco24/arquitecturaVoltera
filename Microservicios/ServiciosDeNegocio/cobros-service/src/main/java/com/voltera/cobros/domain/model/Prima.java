package com.voltera.cobros.domain.model;

import java.math.BigDecimal;

/**
 * Value Object <b>Prima</b> del BC Cobros (lámina 02).
 *
 * <p>Representa el importe de la prima que origina el movimiento de cobro, con su
 * periodicidad. Es inmutable y se valida en construcción.</p>
 *
 * @param monto        importe de la prima (> 0)
 * @param periodicidad MENSUAL / TRIMESTRAL / ANUAL
 */
public record Prima(BigDecimal monto, Periodicidad periodicidad) {

    public enum Periodicidad { MENSUAL, TRIMESTRAL, ANUAL }

    public Prima {
        if (monto == null || monto.signum() <= 0) {
            throw new IllegalArgumentException("La prima debe ser mayor que cero");
        }
        if (periodicidad == null) {
            periodicidad = Periodicidad.MENSUAL;
        }
    }

    public static Prima de(BigDecimal monto, Periodicidad periodicidad) {
        return new Prima(monto, periodicidad);
    }
}
