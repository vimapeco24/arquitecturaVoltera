package com.voltera.reaseguro.infrastructure.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Cuerpo de la peticion para inyectar manualmente un evento SiniestroAprobado
 * (demos sin broker). Los campos temporales son opcionales.
 */
public record SimularEventoRequest(
        @NotBlank String eventId,
        @NotBlank String siniestroId,
        @NotBlank String polizaId,
        @NotBlank String prosumidorId,
        @NotNull @PositiveOrZero BigDecimal montoAprobado,
        String descripcion,
        Instant aprobadoEn,
        Instant emitidoEn
) {
}
