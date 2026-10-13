package com.voltera.telemetriacore.domain.event;

import java.time.Instant;

/**
 * Evento consumido desde {@code topic.telemetria.validadas} (emitido por Ingesta).
 */
public record LecturaValidada(
        String eventId,
        String medidorSerial,
        double consumoKwh,
        Instant capturadaEn,
        Instant validadaEn
) {
}
