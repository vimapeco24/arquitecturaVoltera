package com.voltera.ia.infrastructure.rest.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Entrada para registrar la firma 2 (operador) o 3 (prosumidor) sobre una
 * propuesta. {@code identidad} es la identidad verificada del firmante (IAM);
 * {@code razon} es obligatoria en el rechazo y recomendada en la aprobación.
 * Si llegan {@code direccion} y {@code firma}, se verifica con ecrecover y se usa la
 * firma de la billetera; si no, firma la clave custodiada del rol.
 */
public record FirmaRequest(
        @NotBlank String identidad,
        String razon,
        /** Opcional: dirección 0x de la billetera que firmó (p. ej. MetaMask). */
        String direccion,
        /** Opcional: firma EIP-191 (personal_sign) del mensaje canónico, en hex. */
        String firma
) {
    public FirmaRequest(String identidad, String razon) {
        this(identidad, razon, null, null);
    }
}
