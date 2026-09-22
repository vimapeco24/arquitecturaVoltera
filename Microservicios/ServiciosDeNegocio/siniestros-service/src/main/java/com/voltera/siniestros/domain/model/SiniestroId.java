package com.voltera.siniestros.domain.model;

import java.util.Objects;
import java.util.UUID;

/**
 * Value Object: identificador de un siniestro.
 */
public final class SiniestroId {
    private final String valor;

    private SiniestroId(String valor) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("El id de siniestro no puede ser vacio");
        }
        this.valor = valor;
    }

    public static SiniestroId de(String valor) { return new SiniestroId(valor); }
    public static SiniestroId nuevo() { return new SiniestroId(UUID.randomUUID().toString()); }
    public String valor() { return valor; }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SiniestroId s)) return false;
        return valor.equals(s.valor);
    }
    @Override public int hashCode() { return Objects.hash(valor); }
    @Override public String toString() { return valor; }
}
