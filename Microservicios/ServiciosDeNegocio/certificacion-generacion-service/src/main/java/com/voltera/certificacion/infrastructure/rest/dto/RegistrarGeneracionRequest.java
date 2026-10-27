package com.voltera.certificacion.infrastructure.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

/** Registrar una medición de generación (de Telemetría). */
public record RegistrarGeneracionRequest(
        @NotBlank String medidorId,
        @NotBlank String medicionId,
        @Positive long wh) {}
