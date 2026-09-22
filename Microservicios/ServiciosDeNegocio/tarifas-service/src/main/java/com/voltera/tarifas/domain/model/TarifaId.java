package com.voltera.tarifas.domain.model;

import java.util.Objects;
import java.util.UUID;

public final class TarifaId {
    private final String valor;

    private TarifaId(String valor) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("El id de tarifa no puede ser vacio");
        }
        this.valor = valor;
    }

    public static TarifaId de(String valor) { return new TarifaId(valor); }
    public static TarifaId nuevo() { return new TarifaId(UUID.randomUUID().toString()); }
    public String valor() { return valor; }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TarifaId t)) return false;
        return valor.equals(t.valor);
    }
    @Override public int hashCode() { return Objects.hash(valor); }
    @Override public String toString() { return valor; }
}
