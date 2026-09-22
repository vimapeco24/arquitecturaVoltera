package com.voltera.telemetria.infrastructure.rest.dto;

import com.voltera.telemetria.domain.model.Medidor;

import java.math.BigDecimal;

public record MedidorResponse(
        String id,
        String prosumidorId,
        BigDecimal umbralKwh,
        String estado,
        String registradoEn
) {
    public static MedidorResponse desde(Medidor m) {
        return new MedidorResponse(
                m.id().valor(),
                m.prosumidorId(),
                m.umbralKwh().valor(),
                m.estado().name(),
                m.registradoEn().toString()
        );
    }
}
