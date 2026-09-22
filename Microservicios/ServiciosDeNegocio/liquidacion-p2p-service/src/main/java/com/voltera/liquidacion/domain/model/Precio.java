package com.voltera.liquidacion.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Value Object: precio por kWh en COP. Inmutable.
 */
public final class Precio {

    private final BigDecimal porKwh;

    private Precio(BigDecimal porKwh) {
        if (porKwh.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("El precio por kWh no puede ser negativo");
        }
        this.porKwh = porKwh.setScale(2, RoundingMode.HALF_UP);
    }

    public static Precio porKwh(BigDecimal valor) {
        Objects.requireNonNull(valor, "valor requerido");
        return new Precio(valor);
    }

    public static Precio porKwh(double valor) {
        return new Precio(BigDecimal.valueOf(valor));
    }

    public boolean menorOIgualQue(Precio otro) {
        return this.porKwh.compareTo(otro.porKwh) <= 0;
    }

    /** Promedio entre dos precios (precio de casacion del emparejamiento). */
    public Precio promedioCon(Precio otro) {
        return new Precio(this.porKwh.add(otro.porKwh)
                .divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP));
    }

    /** Valor total = precio * cantidad de energia. */
    public BigDecimal valorar(Energia energia) {
        return porKwh.multiply(energia.kwh()).setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal porKwh() {
        return porKwh;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Precio p)) return false;
        return porKwh.compareTo(p.porKwh) == 0;
    }

    @Override
    public int hashCode() {
        return porKwh.stripTrailingZeros().hashCode();
    }

    @Override
    public String toString() {
        return "COP " + porKwh.toPlainString() + "/kWh";
    }
}
