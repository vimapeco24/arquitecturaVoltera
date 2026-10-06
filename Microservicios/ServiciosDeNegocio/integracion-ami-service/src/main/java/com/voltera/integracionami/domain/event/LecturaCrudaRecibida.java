package com.voltera.integracionami.domain.event;

import java.time.Instant;

/**
 * Evento canonico emitido por el ACL: una lectura cruda ya traducida al formato
 * estandar de Voltera. La consume el BC Telemetria · Ingesta.
 *
 * @param eventId       id de negocio del evento (idempotencia aguas abajo)
 * @param medidorSerial serial canonico del medidor
 * @param consumoKwh    valor normalizado a kWh (formato canonico)
 * @param proveedorAmi  proveedor de origen
 * @param capturadaEn   timestamp de captura original
 * @param recibidaEn    timestamp de recepcion en el ACL
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
