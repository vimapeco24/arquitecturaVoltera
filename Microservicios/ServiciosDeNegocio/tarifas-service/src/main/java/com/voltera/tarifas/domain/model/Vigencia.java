package com.voltera.tarifas.domain.model;

import java.time.Instant;

/**
 * Value Object <b>Vigencia</b> (diagrama DDD 02): ventana temporal durante la cual
 * una Tarifa esta vigente para un medidor. {@code hasta == null} => vigente indefinida.
 */
public record Vigencia(Instant desde, Instant hasta) {

    public static Vigencia desde(Instant desde) {
        return new Vigencia(desde != null ? desde : Instant.now(), null);
    }

    public boolean vigenteEn(Instant momento) {
        if (momento.isBefore(desde)) return false;
        return hasta == null || momento.isBefore(hasta);
    }
}
