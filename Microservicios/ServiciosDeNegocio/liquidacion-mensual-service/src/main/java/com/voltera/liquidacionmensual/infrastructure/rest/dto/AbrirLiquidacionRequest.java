package com.voltera.liquidacionmensual.infrastructure.rest.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

/**
 * Abre una liquidacion mensual. Los precios (COP/kWh) son OPCIONALES: si no se
 * envian, el dominio aplica valores por defecto. Con ellos, al cerrar se calcula
 * el valor economico de la liquidacion (cargo, credito y neto en COP).
 */
public record AbrirLiquidacionRequest(
        @NotBlank(message = "prosumidorId es obligatorio")
        String prosumidorId,

        @Min(value = 2000, message = "anio invalido")
        @Max(value = 2100, message = "anio invalido")
        int anio,

        @Min(value = 1, message = "mes debe estar entre 1 y 12")
        @Max(value = 12, message = "mes debe estar entre 1 y 12")
        int mes,

        @PositiveOrZero(message = "precioConsumoKwh debe ser >= 0")
        BigDecimal precioConsumoKwh,

        @PositiveOrZero(message = "precioExcedenteKwh debe ser >= 0")
        BigDecimal precioExcedenteKwh
) {}
