package com.voltera.liquidacionmensual.domain.model;

import java.time.YearMonth;
import java.util.Objects;

/**
 * Value Object: periodo mensual (ano-mes) que cubre la liquidacion.
 */
public final class Periodo {

    private final int anio;
    private final int mes;

    private Periodo(int anio, int mes) {
        if (mes < 1 || mes > 12) {
            throw new IllegalArgumentException("El mes debe estar entre 1 y 12");
        }
        if (anio < 2000 || anio > 2100) {
            throw new IllegalArgumentException("El anio debe estar entre 2000 y 2100");
        }
        this.anio = anio;
        this.mes = mes;
    }

    public static Periodo de(int anio, int mes) {
        return new Periodo(anio, mes);
    }

    public YearMonth aYearMonth() {
        return YearMonth.of(anio, mes);
    }

    public int anio() { return anio; }
    public int mes() { return mes; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Periodo p)) return false;
        return anio == p.anio && mes == p.mes;
    }

    @Override
    public int hashCode() {
        return Objects.hash(anio, mes);
    }

    @Override
    public String toString() {
        return String.format("%04d-%02d", anio, mes);
    }
}
