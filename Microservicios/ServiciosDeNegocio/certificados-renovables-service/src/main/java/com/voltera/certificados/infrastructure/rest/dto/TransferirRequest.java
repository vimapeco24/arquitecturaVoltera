package com.voltera.certificados.infrastructure.rest.dto;

import jakarta.validation.constraints.NotBlank;

/** Transferir la titularidad de un certificado a otra billetera. */
public record TransferirRequest(@NotBlank String hacia) {}
