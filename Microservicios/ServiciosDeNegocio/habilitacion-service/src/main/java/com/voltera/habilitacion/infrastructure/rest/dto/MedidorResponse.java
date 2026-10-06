package com.voltera.habilitacion.infrastructure.rest.dto;

import com.voltera.habilitacion.domain.model.Medidor;

import java.time.Instant;

public record MedidorResponse(
        String medidorId,
        String serial,
        String fabricante,
        String codigoPunto,
        String direccion,
        String ordenInstalacionId,
        String estado,
        boolean canalIngestaCreado,
        boolean tarifaAsignada,
        Instant habilitadoEn,
        Instant finalizadoEn,
        String motivoFallo,
        long timeoutMinutos
) {
    public static MedidorResponse desde(Medidor m) {
        return new MedidorResponse(
                m.medidorId(),
                m.identidad().serial(),
                m.identidad().fabricante(),
                m.punto().codigoPunto(),
                m.punto().direccion(),
                m.ordenInstalacionId(),
                m.estado().name(),
                m.canalIngestaCreado(),
                m.tarifaAsignada(),
                m.habilitadoEn(),
                m.finalizadoEn(),
                m.motivoFallo(),
                Medidor.TIMEOUT_ALTA.toMinutes()
        );
    }
}
