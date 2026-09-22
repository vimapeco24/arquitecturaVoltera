package com.voltera.reaseguro.infrastructure.rest.dto;

import java.math.BigDecimal;

/**
 * Estadisticas agregadas del lado de lectura.
 */
public record StatsResponse(
        long totalCesiones,
        long totalCedidas,
        long totalPendientes,
        long totalRechazadas,
        BigDecimal montoTotalAprobado,
        BigDecimal montoTotalCedido
) {
}
