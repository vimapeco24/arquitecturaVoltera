package com.voltera.telemetriacore.domain.model;

import java.time.Instant;

/**
 * Value Object: una lectura validada incorporada a la serie (append-only).
 */
public record Lectura(double consumoKwh, Instant capturadaEn) {
    public Lectura {
        if (capturadaEn == null) capturadaEn = Instant.now();
    }
}
