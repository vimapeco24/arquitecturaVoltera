package com.voltera.volumetria.domain.model;

import java.util.Objects;

/**
 * Value Object: identificador del medidor que origina la lectura.
 */
public final class MedidorId {

    private final String valor;

    private MedidorId(String valor) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("El id de medidor no puede ser vacio");
        }
        this.valor = valor;
    }

    public static MedidorId de(String valor) {
        return new MedidorId(valor);
    }

    public String valor() {
        return valor;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MedidorId m)) return false;
        return valor.equals(m.valor);
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
