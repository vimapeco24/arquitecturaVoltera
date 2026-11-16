package com.voltera.ia.infrastructure.rest.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Entrada para registrar la firma 2 (operador) o 3 (prosumidor) sobre una
 * propuesta. {@code identidad} es la identidad verificada del firmante (IAM);
 * {@code razon} es obligatoria en el rechazo y recomendada en la aprobación.
 */
public record FirmaRequest(
        @NotBlank String identidad,
        String razon
) {
}
