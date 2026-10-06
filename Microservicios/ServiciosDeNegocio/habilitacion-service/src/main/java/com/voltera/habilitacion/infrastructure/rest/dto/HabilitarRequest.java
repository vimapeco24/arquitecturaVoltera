package com.voltera.habilitacion.infrastructure.rest.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request para disparar el alta vía REST (equivalente a recibir OrdenInstalacionCerrada).
 */
public record HabilitarRequest(
        @NotBlank(message = "medidorId es obligatorio") String medidorId,
        @NotBlank(message = "serial es obligatorio") String serial,
        String fabricante,
        @NotBlank(message = "codigoPunto es obligatorio") String codigoPunto,
        String direccion,
        String ordenInstalacionId
) {
}
