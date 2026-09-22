package com.voltera.siniestros.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Evento de dominio: se emite cuando un siniestro es APROBADO.
 * El eventId (UUID) es la clave de idempotencia para el consumidor.
 */
public record SiniestroAprobado(
        UUID eventId,
        String siniestroId,
        String polizaId,
        String prosumidorId,
        BigDecimal montoAprobado,
        String descripcion,
        Instant aprobadoEn,
        Instant emitidoEn
) {
    public static SiniestroAprobado desde(Siniestro siniestro) {
        Instant ahora = Instant.now();
        return new SiniestroAprobado(
                UUID.randomUUID(),
                siniestro.id().valor(),
                siniestro.polizaId(),
                siniestro.prosumidorId(),
                siniestro.montoReclamacion().valor(),
                siniestro.descripcion(),
                ahora,
                ahora
        );
    }
}
