package com.voltera.ingesta.infrastructure.rest.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.Instant;

/**
 * Request para inyectar una LecturaCrudaRecibida vía REST (prueba sin broker).
 */
public record LecturaRequest(
        @NotBlank(message = "medidorSerial es obligatorio") String medidorSerial,
        double consumoKwh,
        Instant capturadaEn
) {
}
