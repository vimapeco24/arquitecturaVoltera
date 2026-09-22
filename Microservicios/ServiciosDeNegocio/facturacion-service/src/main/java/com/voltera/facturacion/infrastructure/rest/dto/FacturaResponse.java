package com.voltera.facturacion.infrastructure.rest.dto;

import com.voltera.facturacion.domain.model.Factura;

import java.math.BigDecimal;

/**
 * DTO de salida del adaptador REST. Traduce el agregado de dominio a una
 * representacion serializable sin exponer la estructura interna del modelo.
 */
public record FacturaResponse(
        String id,
        String prosumidorId,
        String periodoInicio,
        String periodoFin,
        BigDecimal kwhConsumidos,
        BigDecimal kwhInyectados,
        BigDecimal consumoNetoKwh,
        BigDecimal excedenteKwh,
        BigDecimal cargoConsumo,
        BigDecimal creditoExcedente,
        BigDecimal total,
        boolean saldoAFavor,
        String estado,
        String emitidaEn
) {
    public static FacturaResponse desde(Factura f) {
        return new FacturaResponse(
                f.id().valor(),
                f.prosumidorId(),
                f.periodo().inicio().toString(),
                f.periodo().fin().toString(),
                f.consumoNeto().kwhConsumidos(),
                f.consumoNeto().kwhInyectados(),
                f.consumoNeto().neto(),
                f.consumoNeto().excedente(),
                f.cargoConsumo().valor(),
                f.creditoExcedente().valor(),
                f.total().valor(),
                f.tieneSaldoAFavor(),
                f.estado().name(),
                f.emitidaEn().toString()
        );
    }
}
