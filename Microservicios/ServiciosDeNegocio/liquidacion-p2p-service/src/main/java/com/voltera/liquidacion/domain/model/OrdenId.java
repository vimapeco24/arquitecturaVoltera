package com.voltera.liquidacion.domain.model;

import java.util.Objects;
import java.util.UUID;

/**
 * Value Object: identificador unico de una OrdenMercado.
 */
public final class OrdenId {

    private final String valor;

    private OrdenId(String valor) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("El id de orden no puede ser vacio");
        }
        this.valor = valor;
    }

    public static OrdenId de(String valor) {
        return new OrdenId(valor);
    }

    public static OrdenId nuevo() {
        return new OrdenId(UUID.randomUUID().toString());
    }

    public String valor() {
        return valor;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OrdenId i)) return false;
        return valor.equals(i.valor);
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
