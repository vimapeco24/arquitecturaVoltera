package com.voltera.reaseguro.domain.event;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Evento de integracion consumido desde el topic {@code siniestro-aprobado}
 * (emitido por siniestros-service). Transporta el estado necesario para crear la
 * cesion sin necesidad de consultar al servicio origen (transferencia de estado).
 */
public record SiniestroAprobado(
        String eventId,
        String siniestroId,
        String polizaId,
        String prosumidorId,
        BigDecimal montoAprobado,
        String descripcion,
        Instant aprobadoEn,
        Instant emitidoEn
) {
}
