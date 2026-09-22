package com.voltera.volumetria.infrastructure.rest.dto;

import com.voltera.volumetria.domain.model.LecturaTelemetria;

import java.math.BigDecimal;

public record LecturaResponse(
        String id,
        String medidorId,
        BigDecimal kwh,
        String direccion,
        String estado,
        boolean valida,
        String capturadaEn
) {
    public static LecturaResponse desde(LecturaTelemetria l) {
        return new LecturaResponse(
                l.id().valor(),
                l.medidorId().valor(),
                l.medida().kwh(),
                l.medida().direccion().name(),
                l.estado().name(),
                l.esValida(),
                l.capturadaEn().toString()
        );
    }
}
