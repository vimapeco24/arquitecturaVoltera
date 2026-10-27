package com.voltera.certificacion.infrastructure.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

/** Solicitar la certificación de un bloque reservado. */
public record SolicitarRequest(
        @NotBlank String medidorId,
        @NotBlank String propietario,
        @Positive int periodo) {}
