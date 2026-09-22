package com.voltera.volumetria.infrastructure.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.Instant;

public record IngestarLecturaRequest(
        @NotBlank(message = "medidorId es obligatorio")
        String medidorId,

        @NotNull @PositiveOrZero(message = "kwh debe ser >= 0")
        BigDecimal kwh,

        @NotBlank(message = "direccion es obligatoria (CONSUMO|GENERACION)")
        String direccion,

        @NotNull(message = "capturadaEn es obligatorio")
        Instant capturadaEn
) {}
