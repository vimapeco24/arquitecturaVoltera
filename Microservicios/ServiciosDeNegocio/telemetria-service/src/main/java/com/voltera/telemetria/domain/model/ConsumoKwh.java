package com.voltera.telemetria.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Value Object: energia consumida en kilovatios-hora (kWh), no negativa.
 */
public final class ConsumoKwh {

    private final BigDecimal valor;

    private ConsumoKwh(BigDecimal valor) {
        if (valor.signum() < 0) {
            throw new IllegalArgumentException("El consumo en kWh no puede ser negativo");
        }
        this.valor = valor.setScale(3, RoundingMode.HALF_UP);
    }

    public static ConsumoKwh de(BigDecimal valor) {
        Objects.requireNonNull(valor, "valor requerido");
        return new ConsumoKwh(valor);
    }

    public static ConsumoKwh de(double valor) {
        return new ConsumoKwh(BigDecimal.valueOf(valor));
    }

    public BigDecimal valor() { return valor; }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ConsumoKwh c)) return false;
        return valor.compareTo(c.valor) == 0;
    }
    @Override public int hashCode() { return valor.stripTrailingZeros().hashCode(); }
    @Override public String toString() { return valor.toPlainString() + " kWh"; }
}
