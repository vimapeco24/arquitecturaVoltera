package com.voltera.tarifaeventos.infrastructure.rest.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request para iniciar el alta de un medidor (orquestacion).
 */
public record IniciarAltaRequest(
        @NotBlank(message = "medidorId es obligatorio") String medidorId,
        String prosumidorId
) {
}
