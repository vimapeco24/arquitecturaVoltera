package com.voltera.telemetriacore.domain.event;

import java.time.Instant;

/**
 * Evento consumido desde {@code telemetria-lecturas-validadas} (emitido por Ingesta).
 */
public record LecturaValidada(
        String eventId,
        String medidorSerial,
        double consumoKwh,
        Instant capturadaEn,
        Instant validadaEn
) {
}
