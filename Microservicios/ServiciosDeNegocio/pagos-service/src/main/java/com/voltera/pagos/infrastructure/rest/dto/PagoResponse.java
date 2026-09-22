package com.voltera.pagos.infrastructure.rest.dto;

import com.voltera.pagos.domain.model.OrdenPago;

import java.math.BigDecimal;

public record PagoResponse(
        String id,
        String prosumidorId,
        String referencia,
        String tipo,
        BigDecimal monto,
        String estado,
        String motivoFallo,
        String creadaEn
) {
    public static PagoResponse desde(OrdenPago o) {
        return new PagoResponse(
                o.id().valor(),
                o.prosumidorId(),
                o.referencia(),
                o.tipo().name(),
                o.monto().valor(),
                o.estado().name(),
                o.motivoFallo(),
                o.creadaEn().toString()
        );
    }
}
