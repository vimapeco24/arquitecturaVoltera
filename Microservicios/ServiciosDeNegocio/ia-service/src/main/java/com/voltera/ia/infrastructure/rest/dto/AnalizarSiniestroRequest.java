package com.voltera.ia.infrastructure.rest.dto;

import com.voltera.ia.domain.model.SolicitudCopiloto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

/**
 * Cuerpo de entrada al Copiloto de Siniestros. {@code disparadoPorEvento}
 * distingue si el flujo lo despertó un evento del broker o una consulta manual.
 */
public record AnalizarSiniestroRequest(
        String siniestroId,
        @NotBlank String polizaId,
        String prosumidorId,
        @NotBlank String descripcion,
        @PositiveOrZero BigDecimal montoReclamacion,
        Boolean disparadoPorEvento
) {
    public SolicitudCopiloto aDominio() {
        return new SolicitudCopiloto(
                siniestroId,
                polizaId,
                prosumidorId,
                descripcion,
                montoReclamacion == null ? BigDecimal.ZERO : montoReclamacion,
                Boolean.TRUE.equals(disparadoPorEvento));
    }
}
