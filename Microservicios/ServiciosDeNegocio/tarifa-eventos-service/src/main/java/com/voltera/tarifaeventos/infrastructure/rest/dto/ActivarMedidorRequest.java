package com.voltera.tarifaeventos.infrastructure.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record ActivarMedidorRequest(
        @NotBlank(message = "medidorId es obligatorio")
        String medidorId,

        @NotBlank(message = "prosumidorId es obligatorio")
        String prosumidorId,

        @NotNull @Positive(message = "umbralKwh debe ser > 0")
        BigDecimal umbralKwh
) {}
