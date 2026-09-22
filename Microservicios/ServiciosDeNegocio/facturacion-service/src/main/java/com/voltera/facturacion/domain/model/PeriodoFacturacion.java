package com.voltera.facturacion.domain.model;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Value Object: intervalo sobre el que se calcula la factura.
 */
public final class PeriodoFacturacion {

    private final LocalDate inicio;
    private final LocalDate fin;

    private PeriodoFacturacion(LocalDate inicio, LocalDate fin) {
        if (fin.isBefore(inicio)) {
            throw new IllegalArgumentException("El fin del periodo no puede ser anterior al inicio");
        }
        this.inicio = inicio;
        this.fin = fin;
    }

    public static PeriodoFacturacion de(LocalDate inicio, LocalDate fin) {
        Objects.requireNonNull(inicio, "inicio requerido");
        Objects.requireNonNull(fin, "fin requerido");
        return new PeriodoFacturacion(inicio, fin);
    }

    public LocalDate inicio() {
        return inicio;
    }

    public LocalDate fin() {
        return fin;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PeriodoFacturacion p)) return false;
        return inicio.equals(p.inicio) && fin.equals(p.fin);
    }

    @Override
    public int hashCode() {
        return Objects.hash(inicio, fin);
    }

    @Override
    public String toString() {
        return inicio + " a " + fin;
    }
}
