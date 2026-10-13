package com.voltera.siniestros.infrastructure.rest.dto;

import com.voltera.siniestros.domain.port.in.DrenarBufferUseCase.ResultadoDrenado;

/**
 * Vista REST del resultado de drenar el buffer offline (store-and-forward).
 */
public record BufferResponse(int reenviados, int pendientes) {
    public static BufferResponse desde(ResultadoDrenado r) {
        return new BufferResponse(r.reenviados(), r.pendientes());
    }
}
