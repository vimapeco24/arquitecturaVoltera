package com.voltera.liquidacion.infrastructure.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

/**
 * DTO de entrada del adaptador REST para emparejar dos ordenes.
 */
public record EmparejarRequest(
        @NotBlank(message = "vendedorId es obligatorio")
        String vendedorId,

        @NotNull @Positive(message = "kwhVenta debe ser > 0")
        BigDecimal kwhVenta,

        @NotNull @PositiveOrZero(message = "precioVenta debe ser >= 0")
        BigDecimal precioVenta,

        @NotBlank(message = "compradorId es obligatorio")
        String compradorId,

        @NotNull @Positive(message = "kwhCompra debe ser > 0")
        BigDecimal kwhCompra,

        @NotNull @PositiveOrZero(message = "precioCompra debe ser >= 0")
        BigDecimal precioCompra
) {}
