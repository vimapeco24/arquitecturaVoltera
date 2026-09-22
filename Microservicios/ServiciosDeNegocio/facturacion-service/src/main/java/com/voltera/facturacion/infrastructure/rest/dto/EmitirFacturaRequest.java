package com.voltera.facturacion.infrastructure.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO de entrada del adaptador REST. Se valida en el borde y se traduce
 * al comando de dominio, sin filtrar detalles de transporte hacia el nucleo.
 */
public record EmitirFacturaRequest(
        @NotBlank(message = "prosumidorId es obligatorio")
        String prosumidorId,

        @NotNull(message = "periodoInicio es obligatorio")
        LocalDate periodoInicio,

        @NotNull(message = "periodoFin es obligatorio")
        LocalDate periodoFin,

        @NotNull @PositiveOrZero(message = "kwhConsumidos debe ser >= 0")
        BigDecimal kwhConsumidos,

        @NotNull @PositiveOrZero(message = "kwhInyectados debe ser >= 0")
        BigDecimal kwhInyectados,

        @NotNull @PositiveOrZero(message = "precioConsumoKwh debe ser >= 0")
        BigDecimal precioConsumoKwh,

        @NotNull @PositiveOrZero(message = "precioExcedenteKwh debe ser >= 0")
        BigDecimal precioExcedenteKwh,

        // OPCIONALES: si se envía tarifaId, facturación consulta el precio al
        // servicio de Tarifas (salto real entre micros). hora por defecto = 0.
        String tarifaId,

        Integer hora
) {}
