package com.voltera.facturacion.domain.model;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Value Object: esquema de precio por kWh.
 * precioConsumo: lo que paga el prosumidor por kWh tomado de la red.
 * precioExcedente: lo que se le reconoce por kWh inyectado.
 */
public final class Tarifa {

    private final Dinero precioConsumoKwh;
    private final Dinero precioExcedenteKwh;

    private Tarifa(Dinero precioConsumoKwh, Dinero precioExcedenteKwh) {
        if (precioConsumoKwh.esNegativo() || precioExcedenteKwh.esNegativo()) {
            throw new IllegalArgumentException("Los precios de tarifa no pueden ser negativos");
        }
        this.precioConsumoKwh = precioConsumoKwh;
        this.precioExcedenteKwh = precioExcedenteKwh;
    }

    public static Tarifa de(Dinero precioConsumoKwh, Dinero precioExcedenteKwh) {
        Objects.requireNonNull(precioConsumoKwh, "precioConsumoKwh requerido");
        Objects.requireNonNull(precioExcedenteKwh, "precioExcedenteKwh requerido");
        return new Tarifa(precioConsumoKwh, precioExcedenteKwh);
    }

    public Dinero valorarConsumo(BigDecimal kwh) {
        return precioConsumoKwh.multiplicar(kwh);
    }

    public Dinero valorarExcedente(BigDecimal kwh) {
        return precioExcedenteKwh.multiplicar(kwh);
    }

    public Dinero precioConsumoKwh() {
        return precioConsumoKwh;
    }

    public Dinero precioExcedenteKwh() {
        return precioExcedenteKwh;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Tarifa t)) return false;
        return precioConsumoKwh.equals(t.precioConsumoKwh)
                && precioExcedenteKwh.equals(t.precioExcedenteKwh);
    }

    @Override
    public int hashCode() {
        return Objects.hash(precioConsumoKwh, precioExcedenteKwh);
    }
}
