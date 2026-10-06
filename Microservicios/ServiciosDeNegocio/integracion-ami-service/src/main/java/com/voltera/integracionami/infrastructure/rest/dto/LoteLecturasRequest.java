package com.voltera.integracionami.infrastructure.rest.dto;

import jakarta.validation.constraints.NotEmpty;

import java.time.Instant;
import java.util.List;

/**
 * Request para recibir un lote de lecturas crudas del head-end.
 */
public record LoteLecturasRequest(
        String proveedorAmi,
        @NotEmpty(message = "el lote no puede estar vacio") List<LecturaCrudaDto> lecturas
) {
    public record LecturaCrudaDto(String medidorSerial, double valor, String unidad, Instant capturadaEn) {}
}
