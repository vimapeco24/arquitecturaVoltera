package com.voltera.tarifaeventos.domain.event;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Evento de integracion consumido desde el topic {@code consumo-registrado}
 * (emitido por telemetria-service). Transporta el estado necesario para calcular
 * el cargo tarifario sin consultar al servicio origen (transferencia de estado).
 */
public record ConsumoRegistrado(
        String eventId,
        String medidorId,
        String prosumidorId,
        BigDecimal consumoKwh,
        BigDecimal umbralKwh,
        boolean consumoExtra,
        Instant ocurridoEn,
        Instant emitidoEn
) {
}
