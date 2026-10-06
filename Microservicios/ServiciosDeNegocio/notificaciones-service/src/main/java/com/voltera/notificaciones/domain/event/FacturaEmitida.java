package com.voltera.notificaciones.domain.event;

/**
 * Evento de entrada (diagrama DDD 02): el BC Notificaciones consume
 * {@code FacturaEmitida} (de Facturacion) para avisar al cliente que su factura
 * del periodo fue emitida.
 */
public record FacturaEmitida(String facturaId, String medidorSerial, String periodo, double monto) {
}
