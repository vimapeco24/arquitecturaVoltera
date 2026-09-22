package com.voltera.facturacion.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Value Object: cantidad monetaria inmutable en COP.
 * Dos objetos con el mismo valor son intercambiables.
 */
public final class Dinero {

    private final BigDecimal valor;

    private Dinero(BigDecimal valor) {
        this.valor = valor.setScale(2, RoundingMode.HALF_UP);
    }

    public static Dinero de(BigDecimal valor) {
        Objects.requireNonNull(valor, "El valor monetario no puede ser nulo");
        return new Dinero(valor);
    }

    public static Dinero de(double valor) {
        return new Dinero(BigDecimal.valueOf(valor));
    }

    public static Dinero cero() {
        return new Dinero(BigDecimal.ZERO);
    }

    public Dinero sumar(Dinero otro) {
        return new Dinero(this.valor.add(otro.valor));
    }

    public Dinero restar(Dinero otro) {
        return new Dinero(this.valor.subtract(otro.valor));
    }

    public Dinero multiplicar(BigDecimal factor) {
        return new Dinero(this.valor.multiply(factor));
    }

    public boolean esNegativo() {
        return this.valor.compareTo(BigDecimal.ZERO) < 0;
    }

    public BigDecimal valor() {
        return valor;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Dinero dinero)) return false;
        return valor.compareTo(dinero.valor) == 0;
    }

    @Override
    public int hashCode() {
        return valor.stripTrailingZeros().hashCode();
    }

    @Override
    public String toString() {
        return "COP " + valor.toPlainString();
    }
}
