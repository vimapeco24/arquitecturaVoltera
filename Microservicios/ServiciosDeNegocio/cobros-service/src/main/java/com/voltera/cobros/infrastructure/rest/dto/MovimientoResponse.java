package com.voltera.cobros.infrastructure.rest.dto;

import com.voltera.cobros.domain.model.Movimiento;

import java.math.BigDecimal;

/**
 * Vista REST de un {@link Movimiento} de cobro.
 */
public record MovimientoResponse(
        String id,
        String clienteId,
        String polizaId,
        BigDecimal montoPrima,
        String periodicidad,
        String estado,
        String reciboNumero,
        BigDecimal montoPagado,
        String registradoEn,
        String conciliadoEn
) {
    public static MovimientoResponse desde(Movimiento m) {
        return new MovimientoResponse(
                m.id().valor(),
                m.clienteId(),
                m.polizaId(),
                m.prima().monto(),
                m.prima().periodicidad().name(),
                m.estado().name(),
                m.recibo() != null ? m.recibo().numero() : null,
                m.recibo() != null ? m.recibo().montoPagado() : null,
                m.registradoEn() != null ? m.registradoEn().toString() : null,
                m.conciliadoEn() != null ? m.conciliadoEn().toString() : null
        );
    }
}
