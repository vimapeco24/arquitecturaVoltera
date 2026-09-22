package com.voltera.tarifaeventos.infrastructure.rest.dto;

import java.math.BigDecimal;

public record StatsResponse(
        int totalCargos,
        long totalLiquidados,
        long totalSinCargo,
        BigDecimal montoTotalCargado,
        BigDecimal excedenteTotalKwh
) {}
