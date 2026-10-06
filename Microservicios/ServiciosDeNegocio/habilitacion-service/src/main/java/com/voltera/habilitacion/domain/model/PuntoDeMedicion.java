package com.voltera.habilitacion.domain.model;

/**
 * Value Object: punto de medicion (ubicacion fisica del medidor en la red).
 */
public record PuntoDeMedicion(String codigoPunto, String direccion) {
    public PuntoDeMedicion {
        if (codigoPunto == null || codigoPunto.isBlank()) {
            throw new IllegalArgumentException("El codigo del punto de medicion es obligatorio");
        }
    }
}
