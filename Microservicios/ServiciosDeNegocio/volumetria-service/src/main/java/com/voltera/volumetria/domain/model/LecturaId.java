package com.voltera.volumetria.domain.model;

import java.util.Objects;
import java.util.UUID;

/**
 * Value Object: identificador unico de una LecturaTelemetria.
 */
public final class LecturaId {

    private final String valor;

    private LecturaId(String valor) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("El id de lectura no puede ser vacio");
        }
        this.valor = valor;
    }

    public static LecturaId de(String valor) {
        return new LecturaId(valor);
    }

    public static LecturaId nuevo() {
        return new LecturaId(UUID.randomUUID().toString());
    }

    public String valor() {
        return valor;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LecturaId l)) return false;
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
