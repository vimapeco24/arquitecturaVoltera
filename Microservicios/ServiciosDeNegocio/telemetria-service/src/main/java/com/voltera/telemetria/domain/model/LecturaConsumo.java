package com.voltera.telemetria.domain.model;

import java.time.Instant;

/**
 * Value Object: una lectura instantanea de consumo capturada por el medidor IoT.
 * Es inmutable y auto-validante.
 */
public record LecturaConsumo(ConsumoKwh consumo, Instant capturadaEn) {

    public LecturaConsumo {
        if (consumo == null) {
            throw new IllegalArgumentException("La lectura requiere un consumo");
        }
        if (capturadaEn == null) {
            capturadaEn = Instant.now();
        }
    }

    public static LecturaConsumo de(double kwh, Instant capturadaEn) {
        return new LecturaConsumo(ConsumoKwh.de(kwh), capturadaEn);
    }
}
