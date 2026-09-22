package com.voltera.reaseguro.domain.model;

import java.util.Objects;
import java.util.UUID;

/**
 * Value Object que identifica de forma unica una CesionRiesgo.
 */
public record CesionId(String valor) {

    public CesionId {
        Objects.requireNonNull(valor, "El id de la cesion no puede ser nulo");
        if (valor.isBlank()) {
            throw new IllegalArgumentException("El id de la cesion no puede estar vacio");
        }
    }

    public static CesionId nuevo() {
        return new CesionId(UUID.randomUUID().toString());
    }

    public static CesionId de(String valor) {
        return new CesionId(valor);
    }

    @Override
    public String toString() {
        return valor;
    }
}
