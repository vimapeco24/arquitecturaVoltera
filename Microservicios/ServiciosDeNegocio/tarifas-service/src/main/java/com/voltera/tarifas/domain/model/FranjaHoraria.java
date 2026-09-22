package com.voltera.tarifas.domain.model;

import java.util.Objects;

/**
 * Value Object: franja horaria con su rango [horaInicio, horaFin) y el precio
 * aplicable por kWh en esa franja. Base de la tarifa dinamica.
 */
public final class FranjaHoraria {

    private final int horaInicio;  // 0..23
    private final int horaFin;     // 1..24 (exclusivo)
    private final Precio precio;

    private FranjaHoraria(int horaInicio, int horaFin, Precio precio) {
        if (horaInicio < 0 || horaInicio > 23) {
            throw new IllegalArgumentException("horaInicio debe estar entre 0 y 23");
        }
        if (horaFin < 1 || horaFin > 24) {
            throw new IllegalArgumentException("horaFin debe estar entre 1 y 24");
        }
        if (horaFin <= horaInicio) {
            throw new IllegalArgumentException("horaFin debe ser mayor que horaInicio");
        }
        this.horaInicio = horaInicio;
        this.horaFin = horaFin;
        this.precio = precio;
    }

    public static FranjaHoraria de(int horaInicio, int horaFin, Precio precio) {
        Objects.requireNonNull(precio, "precio requerido");
        return new FranjaHoraria(horaInicio, horaFin, precio);
    }

    public boolean cubre(int hora) {
        return hora >= horaInicio && hora < horaFin;
    }

    public boolean seSolapaCon(FranjaHoraria otra) {
        return this.horaInicio < otra.horaFin && otra.horaInicio < this.horaFin;
    }

    public int horaInicio() { return horaInicio; }
    public int horaFin() { return horaFin; }
    public Precio precio() { return precio; }
}
