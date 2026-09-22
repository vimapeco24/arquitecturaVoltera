package com.voltera.siniestros.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Value Object: monto de reclamacion de un siniestro, positivo en COP.
 */
public final class MontoReclamacion {

    private final BigDecimal valor;

    private MontoReclamacion(BigDecimal valor) {
        if (valor.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El monto de reclamacion debe ser mayor a cero");
        }
        this.valor = valor.setScale(2, RoundingMode.HALF_UP);
    }

    public static MontoReclamacion de(BigDecimal valor) {
        Objects.requireNonNull(valor, "valor requerido");
        return new MontoReclamacion(valor);
    }

    public static MontoReclamacion de(double valor) {
        return new MontoReclamacion(BigDecimal.valueOf(valor));
    }

    public BigDecimal valor() { return valor; }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MontoReclamacion m)) return false;
        return valor.compareTo(m.valor) == 0;
    }
    @Override public int hashCode() { return valor.stripTrailingZeros().hashCode(); }
    @Override public String toString() { return "COP " + valor.toPlainString(); }
}
