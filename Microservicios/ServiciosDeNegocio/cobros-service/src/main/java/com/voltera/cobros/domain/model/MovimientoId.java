package com.voltera.cobros.domain.model;

import java.util.UUID;

/**
 * Identificador del agregado {@link Movimiento} (BC Cobros).
 */
public record MovimientoId(String valor) {

    public MovimientoId {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("MovimientoId es obligatorio");
        }
    }

    public static MovimientoId nuevo() {
        return new MovimientoId("MOV-" + UUID.randomUUID());
    }

    public static MovimientoId de(String valor) {
        return new MovimientoId(valor);
    }
}
