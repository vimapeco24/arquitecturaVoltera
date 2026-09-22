package com.voltera.tarifas.infrastructure.rest.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.util.List;

public record CrearTarifaRequest(
        @NotBlank(message = "nombre es obligatorio")
        String nombre,

        @NotEmpty(message = "debe incluir al menos una franja")
        @Valid
        List<Franja> franjas
) {
    public record Franja(
            int horaInicio,
            int horaFin,
            @NotNull @PositiveOrZero(message = "precioKwh debe ser >= 0")
            BigDecimal precioKwh
    ) {}
}
