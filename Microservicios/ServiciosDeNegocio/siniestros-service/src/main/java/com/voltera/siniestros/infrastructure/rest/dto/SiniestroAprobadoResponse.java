package com.voltera.siniestros.infrastructure.rest.dto;

import com.voltera.siniestros.domain.model.SiniestroAprobado;

import java.math.BigDecimal;

public record SiniestroAprobadoResponse(
        String eventId,
        String siniestroId,
        String polizaId,
        String prosumidorId,
        BigDecimal montoAprobado,
        String descripcion,
        String aprobadoEn,
        String emitidoEn
) {
    public static SiniestroAprobadoResponse desde(SiniestroAprobado e) {
        return new SiniestroAprobadoResponse(
                e.eventId().toString(),
                e.siniestroId(),
                e.polizaId(),
                e.prosumidorId(),
                e.montoAprobado(),
                e.descripcion(),
                e.aprobadoEn().toString(),
                e.emitidoEn().toString()
        );
    }
}
