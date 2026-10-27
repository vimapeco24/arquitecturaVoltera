package com.voltera.certificados.infrastructure.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

/** Emitir un certificado renovable (el servicio pedirá al gateway acuñar el NFT). */
public record EmitirRequest(
        @NotBlank String idMedidor,
        @NotBlank String emisor,
        @NotBlank String titular,
        @Positive long consumoNetoMensualWh,
        @Positive int periodo,
        String solicitudCertificacionId) {}
