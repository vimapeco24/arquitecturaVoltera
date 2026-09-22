package com.voltera.liquidacionmensual.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Value Object: cantidad de energia en kWh (inmutable, no negativa).
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

    public static Energia cero() {
        return new Energia(BigDecimal.ZERO);
    }

    public Energia sumar(Energia otra) {
        return new Energia(this.kwh.add(otra.kwh));
    }

    /** Resta acotada a cero (el neto de excedente no puede ser negativo aqui). */
    public BigDecimal restarComoBigDecimal(Energia otra) {
        return this.kwh.subtract(otra.kwh);
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
