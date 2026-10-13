package com.voltera.cobros.infrastructure.rest.dto;

import com.voltera.cobros.domain.model.Prima;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/**
 * Petición para registrar un movimiento de cobro de prima.
 */
public record RegistrarMovimientoRequest(
        @NotBlank String clienteId,
        @NotBlank String polizaId,
        @NotNull @Positive BigDecimal montoPrima,
        Prima.Periodicidad periodicidad
) {
}
