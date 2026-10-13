package com.voltera.cobros.infrastructure.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Petición para simular un evento {@code PagoConfirmado} sin broker (demo/experimento
 * de idempotencia): reenviar el mismo {@code eventId} no debe conciliar dos veces.
 */
public record SimularPagoRequest(
        @NotBlank String eventId,
        @NotBlank String referencia,
        @NotBlank String clienteId,
        @NotBlank String polizaId,
        @NotNull @Positive BigDecimal montoPagado,
        Instant confirmadoEn,
        Instant emitidoEn
) {
}
