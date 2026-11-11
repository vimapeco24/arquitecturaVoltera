package com.voltera.ia.infrastructure.rest.dto;

import com.voltera.ia.domain.model.der.AlertaTelemetria;
import com.voltera.ia.domain.model.der.TipoActivo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * Entrada al Agente de diagnóstico DER: una alerta de telemetría de un activo.
 * {@code usuario} es la identidad del operador (IAM: el agente hereda sus permisos).
 */
public record DiagnosticarRequest(
        @NotBlank String activoId,
        @NotNull TipoActivo tipoActivo,
        @NotBlank String comunidadId,
        @NotBlank String metrica,
        BigDecimal valor,
        BigDecimal umbral,
        int minutosSinSenal,
        String descripcion,
        String usuario
) {
    public AlertaTelemetria aDominio() {
        return new AlertaTelemetria(
                activoId, tipoActivo, comunidadId, metrica,
                valor == null ? BigDecimal.ZERO : valor,
                umbral == null ? BigDecimal.ZERO : umbral,
                minutosSinSenal,
                descripcion == null ? "" : descripcion,
                usuario);
    }
}
