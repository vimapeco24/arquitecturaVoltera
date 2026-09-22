package com.voltera.facturacion.domain.model;

import java.util.Objects;
import java.util.UUID;

/**
 * Value Object: identificador unico de una Factura.
 * Se referencia por identificador (regla 3 de agregados DDD).
 */
public final class FacturaId {

    private final String valor;

    private FacturaId(String valor) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("El id de factura no puede ser vacio");
        }
        this.valor = valor;
    }

    public static FacturaId de(String valor) {
        return new FacturaId(valor);
    }

    public static FacturaId nuevo() {
        return new FacturaId(UUID.randomUUID().toString());
    }

    public String valor() {
        return valor;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FacturaId f)) return false;
        return valor.equals(f.valor);
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
