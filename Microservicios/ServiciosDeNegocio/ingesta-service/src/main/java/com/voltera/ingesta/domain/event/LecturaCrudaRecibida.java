package com.voltera.ingesta.domain.event;

import java.time.Instant;

/**
 * Evento consumido desde el topic {@code ami-lecturas-crudas} (emitido por el ACL
 * de Integracion AMI). Formato canonico.
 */
public record LecturaCrudaRecibida(
        String eventId,
        String medidorSerial,
        double consumoKwh,
        String proveedorAmi,
        Instant capturadaEn,
        Instant recibidaEn
) {
}
