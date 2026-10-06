package com.voltera.habilitacion.domain.model;

/**
 * Value Object: identidad del dispositivo medidor (serial + fabricante).
 */
public record IdentidadDispositivo(String serial, String fabricante) {
    public IdentidadDispositivo {
        if (serial == null || serial.isBlank()) {
            throw new IllegalArgumentException("El serial del dispositivo es obligatorio");
        }
        if (fabricante == null || fabricante.isBlank()) {
            fabricante = "DESCONOCIDO";
        }
    }
}
