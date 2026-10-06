package com.voltera.habilitacion.domain.event;

import java.time.Instant;

/**
 * Evento de integracion EXTERNO consumido desde el topic {@code ordenes-instalacion}.
 * Lo publica el sistema de ordenes cuando el tecnico cierra la instalacion; es el
 * disparador del alta del medidor (lamina 01/02).
 */
public record OrdenInstalacionCerrada(
        String eventId,
        String ordenInstalacionId,
        String medidorId,
        String serial,
        String fabricante,
        String codigoPunto,
        String direccion,
        Instant cerradaEn
) {
}
