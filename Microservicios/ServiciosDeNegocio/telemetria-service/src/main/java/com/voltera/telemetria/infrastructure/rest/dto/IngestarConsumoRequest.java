package com.voltera.telemetria.infrastructure.rest.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.Instant;

public record IngestarConsumoRequest(
        @NotNull @PositiveOrZero(message = "consumoKwh debe ser >= 0")
        BigDecimal consumoKwh,

        Instant capturadaEn
) {}
