package com.voltera.liquidacionmensual.domain.model;

import java.util.Objects;
import java.util.UUID;

/**
 * Value Object: identificador unico de una LiquidacionMensual.
 */
public final class LiquidacionId {

    private final String valor;

    private LiquidacionId(String valor) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("El id de liquidacion no puede ser vacio");
        }
        this.valor = valor;
    }

    public static LiquidacionId de(String valor) {
        return new LiquidacionId(valor);
    }

    public static LiquidacionId nuevo() {
        return new LiquidacionId(UUID.randomUUID().toString());
    }

    public String valor() {
        return valor;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LiquidacionId l)) return false;
        return valor.equals(l.valor);
    }

    @Override
    public int hashCode() {
        return Objects.hash(valor);
    }

    @Override
    public String toString() {
        return valor;
    }
}
