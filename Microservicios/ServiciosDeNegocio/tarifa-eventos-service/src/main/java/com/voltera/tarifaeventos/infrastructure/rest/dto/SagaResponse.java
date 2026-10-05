package com.voltera.tarifaeventos.infrastructure.rest.dto;

import com.voltera.tarifaeventos.domain.model.SagaAltaMedidor;

import java.time.Instant;

/**
 * Vista REST del estado de una saga de alta de medidor (process manager).
 */
public record SagaResponse(
        String sagaId,
        String medidorId,
        String prosumidorId,
        String estado,
        boolean canalIngestaCreado,
        boolean tarifaAsignada,
        Instant iniciadaEn,
        Instant finalizadaEn,
        String motivoFallo,
        long timeoutMinutos
) {
    public static SagaResponse desde(SagaAltaMedidor s) {
        return new SagaResponse(
                s.sagaId(),
                s.medidorId(),
                s.prosumidorId(),
                s.estado().name(),
                s.canalIngestaCreado(),
                s.tarifaAsignada(),
                s.iniciadaEn(),
                s.finalizadaEn(),
                s.motivoFallo(),
                SagaAltaMedidor.TIMEOUT.toMinutes()
        );
    }
}
