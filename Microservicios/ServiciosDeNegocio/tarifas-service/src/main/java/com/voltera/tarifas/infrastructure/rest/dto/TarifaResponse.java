package com.voltera.tarifas.infrastructure.rest.dto;

import com.voltera.tarifas.domain.model.Tarifa;

import java.math.BigDecimal;
import java.util.List;

public record TarifaResponse(
        String id,
        String nombre,
        List<FranjaResponse> franjas
) {
    public record FranjaResponse(int horaInicio, int horaFin, BigDecimal precioKwh) {}

    public static TarifaResponse desde(Tarifa t) {
        List<FranjaResponse> fr = t.franjas().stream()
                .map(f -> new FranjaResponse(f.horaInicio(), f.horaFin(), f.precio().porKwh()))
                .toList();
        return new TarifaResponse(t.id().valor(), t.nombre(), fr);
    }
}
