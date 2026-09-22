package com.voltera.liquidacion.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Value Object: cantidad de energia en kWh. Inmutable.
 */
public final class Energia {

    private final BigDecimal kwh;

    private Energia(BigDecimal kwh) {
        if (kwh.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("La energia en kWh no puede ser negativa");
        }
        this.kwh = kwh.setScale(3, RoundingMode.HALF_UP);
    }

    public static Energia deKwh(BigDecimal kwh) {
        Objects.requireNonNull(kwh, "kwh requerido");
        return new Energia(kwh);
    }

    public static Energia deKwh(double kwh) {
        return new Energia(BigDecimal.valueOf(kwh));
    }

    public static Energia cero() {
        return new Energia(BigDecimal.ZERO);
    }

    public Energia restar(Energia otra) {
        return new Energia(this.kwh.subtract(otra.kwh));
    }

    public boolean esCero() {
        return this.kwh.compareTo(BigDecimal.ZERO) == 0;
    }

    public boolean mayorOIgualQue(Energia otra) {
        return this.kwh.compareTo(otra.kwh) >= 0;
    }

    /** Devuelve la menor de las dos cantidades. */
    public Energia minimo(Energia otra) {
        return this.kwh.compareTo(otra.kwh) <= 0 ? this : otra;
    }

    public BigDecimal kwh() {
        return kwh;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Energia e)) return false;
        return kwh.compareTo(e.kwh) == 0;
    }

    @Override
    public int hashCode() {
        return kwh.stripTrailingZeros().hashCode();
    }

    @Override
    public String toString() {
        return kwh.toPlainString() + " kWh";
    }
}
