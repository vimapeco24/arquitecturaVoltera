package com.voltera.siniestros.infrastructure.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ReportarSiniestroRequest(
        @NotBlank(message = "polizaId es obligatorio")
        String polizaId,

        @NotBlank(message = "prosumidorId es obligatorio")
        String prosumidorId,

        @NotBlank(message = "descripcion es obligatoria")
        String descripcion,

        @NotNull @Positive(message = "montoReclamacion debe ser > 0")
        BigDecimal montoReclamacion,

        @NotNull(message = "fechaOcurrencia es obligatoria")
        LocalDate fechaOcurrencia
) {}
