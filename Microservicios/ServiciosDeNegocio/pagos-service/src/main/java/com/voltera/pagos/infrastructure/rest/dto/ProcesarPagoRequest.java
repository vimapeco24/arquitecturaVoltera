package com.voltera.pagos.infrastructure.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record ProcesarPagoRequest(
        @NotBlank(message = "prosumidorId es obligatorio")
        String prosumidorId,

        @NotBlank(message = "referencia es obligatoria")
        String referencia,

        @NotBlank(message = "tipo es obligatorio (COBRO_FACTURA|PAGO_EXCEDENTE)")
        String tipo,

        @NotNull @Positive(message = "monto debe ser > 0")
        BigDecimal monto
) {}
