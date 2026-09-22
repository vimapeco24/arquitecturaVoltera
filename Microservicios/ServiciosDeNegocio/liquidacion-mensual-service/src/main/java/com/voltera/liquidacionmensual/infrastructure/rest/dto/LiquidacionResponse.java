package com.voltera.liquidacionmensual.infrastructure.rest.dto;

import com.voltera.liquidacionmensual.domain.model.LiquidacionMensual;

import java.math.BigDecimal;

/**
 * DTO de salida: expone el resultado de la totalizacion del mes, en ENERGIA
 * (kWh) y en VALOR ECONOMICO (COP). El neto en COP es la base para la factura.
 */
public record LiquidacionResponse(
        String id,
        String prosumidorId,
        String periodo,
        String estado,
        int cantidadMovimientos,
        BigDecimal totalConsumoKwh,
        BigDecimal totalExcedenteKwh,
        BigDecimal netoKwh,
        boolean saldoAFavor,
        // --- Valoracion economica (COP) ---
        BigDecimal precioConsumoKwh,
        BigDecimal precioExcedenteKwh,
        BigDecimal cargoConsumo,
        BigDecimal creditoExcedente,
        BigDecimal netoCop,
        String creadaEn,
        String cerradaEn
) {
    public static LiquidacionResponse desde(LiquidacionMensual l) {
        return new LiquidacionResponse(
                l.id().valor(),
                l.prosumidorId(),
                l.periodo().toString(),
                l.estado().name(),
                l.cantidadMovimientos(),
                l.totalConsumo().kwh(),
                l.totalExcedente().kwh(),
                l.netoKwh(),
                l.tieneSaldoAFavor(),
                l.precioConsumoKwh(),
                l.precioExcedenteKwh(),
                l.cargoConsumo(),
                l.creditoExcedente(),
                l.netoCop(),
                l.creadaEn().toString(),
                l.cerradaEn() != null ? l.cerradaEn().toString() : null
        );
    }
}
