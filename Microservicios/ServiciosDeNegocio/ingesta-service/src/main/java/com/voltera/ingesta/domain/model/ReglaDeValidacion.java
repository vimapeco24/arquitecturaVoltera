package com.voltera.ingesta.domain.model;

/**
 * Value Object: regla de validacion de una lectura (rango valido de kWh por
 * intervalo). Una lectura fuera del rango se marca como sospechosa.
 */
public record ReglaDeValidacion(double minKwh, double maxKwh) {
    public ReglaDeValidacion {
        if (maxKwh < minKwh) {
            throw new IllegalArgumentException("maxKwh no puede ser menor que minKwh");
        }
    }

    public boolean esValida(double consumoKwh) {
        return consumoKwh >= minKwh && consumoKwh <= maxKwh;
    }
}
