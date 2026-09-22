package com.voltera.liquidacionmensual.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

/**
 * Value Object: un movimiento de energia del mes (un consumo o un excedente/inyeccion),
 * proveniente de la volumetria o de las transacciones P2P.
 */
public final class Movimiento {

    public enum Tipo { CONSUMO, EXCEDENTE }

    private final Tipo tipo;
    private final Energia energia;
    private final Instant registradoEn;

    private Movimiento(Tipo tipo, Energia energia, Instant registradoEn) {
        this.tipo = tipo;
        this.energia = energia;
        this.registradoEn = registradoEn;
    }

    public static Movimiento de(Tipo tipo, Energia energia, Instant registradoEn) {
        Objects.requireNonNull(tipo, "tipo requerido");
        Objects.requireNonNull(energia, "energia requerida");
        Objects.requireNonNull(registradoEn, "registradoEn requerido");
        return new Movimiento(tipo, energia, registradoEn);
    }

    public boolean esConsumo() {
        return tipo == Tipo.CONSUMO;
    }

    public boolean esExcedente() {
        return tipo == Tipo.EXCEDENTE;
    }

    public Tipo tipo() { return tipo; }
    public Energia energia() { return energia; }
    public Instant registradoEn() { return registradoEn; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Movimiento m)) return false;
        return tipo == m.tipo
                && energia.equals(m.energia)
                && registradoEn.equals(m.registradoEn);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tipo, energia, registradoEn);
    }
}
