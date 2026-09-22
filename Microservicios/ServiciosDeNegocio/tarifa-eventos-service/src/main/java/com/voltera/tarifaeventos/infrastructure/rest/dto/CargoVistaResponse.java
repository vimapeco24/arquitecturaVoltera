package com.voltera.tarifaeventos.infrastructure.rest.dto;

import com.voltera.tarifaeventos.domain.model.CargoVista;

import java.math.BigDecimal;

public record CargoVistaResponse(
        String id,
        String medidorId,
        String prosumidorId,
        BigDecimal consumoKwh,
        BigDecimal umbralKwh,
        BigDecimal excedenteKwh,
        BigDecimal precioExtraKwh,
        BigDecimal montoCargo,
        String estado,
        String creadoEn
) {
    public static CargoVistaResponse desde(CargoVista v) {
        return new CargoVistaResponse(
                v.id(),
                v.medidorId(),
                v.prosumidorId(),
                v.consumoKwh(),
                v.umbralKwh(),
                v.excedenteKwh(),
                v.precioExtraKwh(),
                v.montoCargo(),
                v.estado().name(),
                v.creadoEn().toString()
        );
    }
}
