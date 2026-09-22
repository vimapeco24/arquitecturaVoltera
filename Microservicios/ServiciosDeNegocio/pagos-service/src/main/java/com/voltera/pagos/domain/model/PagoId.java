package com.voltera.pagos.domain.model;

import java.util.Objects;
import java.util.UUID;

public final class PagoId {
    private final String valor;

    private PagoId(String valor) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("El id de pago no puede ser vacio");
        }
        this.valor = valor;
    }

    public static PagoId de(String valor) { return new PagoId(valor); }
    public static PagoId nuevo() { return new PagoId(UUID.randomUUID().toString()); }
    public String valor() { return valor; }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PagoId p)) return false;
        return valor.equals(p.valor);
    }
    @Override public int hashCode() { return Objects.hash(valor); }
    @Override public String toString() { return valor; }
}
