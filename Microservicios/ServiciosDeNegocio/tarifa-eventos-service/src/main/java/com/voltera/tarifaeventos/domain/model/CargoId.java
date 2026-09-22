package com.voltera.tarifaeventos.domain.model;

import java.util.Objects;
import java.util.UUID;

/**
 * Value Object que identifica de forma unica un CargoTarifa.
 */
public record CargoId(String valor) {

    public CargoId {
        Objects.requireNonNull(valor, "El id del cargo no puede ser nulo");
        if (valor.isBlank()) {
            throw new IllegalArgumentException("El id del cargo no puede estar vacio");
        }
    }

    public static CargoId nuevo() {
        return new CargoId("CAR-" + UUID.randomUUID());
    }

    public static CargoId de(String valor) {
        return new CargoId(valor);
    }

    @Override
    public String toString() {
        return valor;
    }
}
