package com.voltera.siniestros.infrastructure.rest.dto;

import com.voltera.siniestros.domain.model.Siniestro;

import java.math.BigDecimal;

public record SiniestroResponse(
        String id,
        String polizaId,
        String prosumidorId,
        String descripcion,
        BigDecimal montoReclamacion,
        String estado,
        String fechaOcurrencia,
        String reportadoEn
) {
    public static SiniestroResponse desde(Siniestro s) {
        return new SiniestroResponse(
                s.id().valor(),
                s.polizaId(),
                s.prosumidorId(),
                s.descripcion(),
                s.montoReclamacion().valor(),
                s.estado().name(),
                s.fechaOcurrencia().toString(),
                s.reportadoEn().toString()
        );
    }
}
