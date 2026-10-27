package com.voltera.blockchain.infrastructure.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

/** Cuerpo para acuñar un certificado de origen (NFT ERC-721). */
public record EmitirCertificadoRequest(
        @NotBlank String emisor,
        @NotBlank String propietario,
        @NotBlank String idMedidor,
        @Positive long consumoNetoMensualWh,
        @Positive int periodo) {}
