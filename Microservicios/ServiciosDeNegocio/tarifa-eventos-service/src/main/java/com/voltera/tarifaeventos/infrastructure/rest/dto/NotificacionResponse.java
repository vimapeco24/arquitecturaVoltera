package com.voltera.tarifaeventos.infrastructure.rest.dto;

import com.voltera.tarifaeventos.domain.model.NotificacionLiquidada;

import java.math.BigDecimal;

public record NotificacionResponse(
        String notificacionId,
        String medidorId,
        String prosumidorId,
        String tipo,
        BigDecimal cargoFijoMensual,
        BigDecimal precioBaseKwh,
        BigDecimal umbralKwh,
        BigDecimal precioExtraKwh,
        String mensaje,
        String generadaEn
) {
    public static NotificacionResponse desde(NotificacionLiquidada n) {
        return new NotificacionResponse(
                n.notificacionId(),
                n.medidorId(),
                n.prosumidorId(),
                n.tipo(),
                n.cargoFijoMensual(),
                n.precioBaseKwh(),
                n.umbralKwh(),
                n.precioExtraKwh(),
                n.mensaje(),
                n.generadaEn().toString()
        );
    }
}
