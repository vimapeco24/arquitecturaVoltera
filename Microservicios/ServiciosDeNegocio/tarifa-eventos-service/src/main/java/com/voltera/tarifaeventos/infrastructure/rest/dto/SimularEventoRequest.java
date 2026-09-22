package com.voltera.tarifaeventos.infrastructure.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Request para inyectar un evento ConsumoRegistrado sin broker (prueba de
 * idempotencia y de la logica de cargo, de forma determinista).
 */
public record SimularEventoRequest(
        @NotBlank String eventId,
        @NotBlank String medidorId,
        @NotBlank String prosumidorId,
        @NotNull BigDecimal consumoKwh,
        @NotNull BigDecimal umbralKwh,
        boolean consumoExtra,
        Instant ocurridoEn,
        Instant emitidoEn
) {}
