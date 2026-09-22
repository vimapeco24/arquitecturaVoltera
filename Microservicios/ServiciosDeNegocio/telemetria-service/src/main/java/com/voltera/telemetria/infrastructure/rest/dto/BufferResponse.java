package com.voltera.telemetria.infrastructure.rest.dto;

import com.voltera.telemetria.domain.port.in.DrenarBufferUseCase.ResultadoDrenado;

public record BufferResponse(int reenviados, int pendientes) {
    public static BufferResponse desde(ResultadoDrenado r) {
        return new BufferResponse(r.reenviados(), r.pendientes());
    }
}
