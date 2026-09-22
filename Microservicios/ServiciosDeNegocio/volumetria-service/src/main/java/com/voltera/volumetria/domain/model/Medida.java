package com.voltera.volumetria.domain.model;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Value Object: medida de energia en kWh con su direccion (consumo/generacion).
 */
public final class Medida {

    public enum Direccion { CONSUMO, GENERACION }

    private final BigDecimal kwh;
    private final Direccion direccion;

    private Medida(BigDecimal kwh, Direccion direccion) {
        if (kwh.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("La medida en kWh no puede ser negativa");
        }
        this.kwh = kwh;
        this.direccion = direccion;
    }

    public static Medida de(BigDecimal kwh, Direccion direccion) {
        Objects.requireNonNull(kwh, "kwh requerido");
        Objects.requireNonNull(direccion, "direccion requerida");
        return new Medida(kwh, direccion);
    }

    public BigDecimal kwh() {
        return kwh;
    }

    public Direccion direccion() {
        return direccion;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Medida m)) return false;
        return kwh.compareTo(m.kwh) == 0 && direccion == m.direccion;
    }

    @Override
    public int hashCode() {
        return Objects.hash(kwh.stripTrailingZeros(), direccion);
    }
}
