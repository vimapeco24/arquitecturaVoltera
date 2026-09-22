package com.voltera.liquidacion.domain.model;

import java.util.Objects;
import java.util.UUID;

/**
 * Value Object: identificador unico de una TransaccionP2P.
 */
public final class TransaccionId {

    private final String valor;

    private TransaccionId(String valor) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("El id de transaccion no puede ser vacio");
        }
        this.valor = valor;
    }

    public static TransaccionId de(String valor) {
        return new TransaccionId(valor);
    }

    public static TransaccionId nuevo() {
        return new TransaccionId(UUID.randomUUID().toString());
    }

    public String valor() {
        return valor;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TransaccionId t)) return false;
        return valor.equals(t.valor);
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
