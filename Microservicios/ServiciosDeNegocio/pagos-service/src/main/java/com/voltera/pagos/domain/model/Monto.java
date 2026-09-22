package com.voltera.pagos.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Value Object: monto monetario positivo en COP.
 */
public final class Monto {

    private final BigDecimal valor;

    private Monto(BigDecimal valor) {
        if (valor.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El monto debe ser mayor a cero");
        }
        this.valor = valor.setScale(2, RoundingMode.HALF_UP);
    }

    public static Monto de(BigDecimal valor) {
        Objects.requireNonNull(valor, "valor requerido");
        return new Monto(valor);
    }

    public static Monto de(double valor) {
        return new Monto(BigDecimal.valueOf(valor));
    }

    public BigDecimal valor() { return valor; }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Monto m)) return false;
        return valor.compareTo(m.valor) == 0;
    }
    @Override public int hashCode() { return valor.stripTrailingZeros().hashCode(); }
    @Override public String toString() { return "COP " + valor.toPlainString(); }
}
