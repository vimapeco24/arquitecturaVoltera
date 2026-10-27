package com.voltera.blockchain.infrastructure.rest.dto;

import jakarta.validation.constraints.NotBlank;

/** Cuerpo para transferir un certificado de una billetera a otra. */
public record TransferirCertificadoRequest(
        @NotBlank String hacia) {}
