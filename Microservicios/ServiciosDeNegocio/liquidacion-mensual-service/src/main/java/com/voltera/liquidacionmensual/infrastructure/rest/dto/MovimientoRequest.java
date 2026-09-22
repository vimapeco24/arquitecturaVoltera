package com.voltera.liquidacionmensual.infrastructure.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record MovimientoRequest(
        @NotBlank(message = "tipo es obligatorio (CONSUMO|EXCEDENTE)")
        String tipo,

        @NotNull @PositiveOrZero(message = "kwh debe ser >= 0")
        BigDecimal kwh
) {}
