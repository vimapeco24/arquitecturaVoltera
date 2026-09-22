package com.voltera.telemetria.infrastructure.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record RegistrarMedidorRequest(
        @NotBlank(message = "prosumidorId es obligatorio")
        String prosumidorId,

        @NotNull @Positive(message = "umbralKwh debe ser > 0")
        BigDecimal umbralKwh
) {}
