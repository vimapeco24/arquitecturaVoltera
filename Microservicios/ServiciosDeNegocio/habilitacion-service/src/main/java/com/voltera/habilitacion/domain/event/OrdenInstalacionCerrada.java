package com.voltera.habilitacion.domain.event;

import java.time.Instant;

/**
 * Evento de integracion EXTERNO consumido desde el topic {@code ordenes-instalacion}.
 * Lo publica el sistema de ordenes cuando el tecnico cierra la instalacion; es el
 * disparador del alta del medidor (lamina 01/02).
 *
 * <p>Trae ademas los datos comerciales/tecnicos que luego viajan en
 * {@code MedidorHabilitado} (ECST, lamina 03): cliente, proveedor AMI, protocolo,
 * plan tarifario y canales de notificacion. Estos campos son opcionales; el
 * constructor corto se mantiene por compatibilidad.</p>
 */
public record OrdenInstalacionCerrada(
        String eventId,
        String ordenInstalacionId,
        String medidorId,
        String serial,
        String fabricante,
        String codigoPunto,
        String direccion,
        Instant cerradaEn,
        String clienteId,
        String proveedorAmi,
        String protocolo,
        String plan,
        java.util.List<String> canales
) {
    /** Constructor de compatibilidad: orden sin datos comerciales explicitos. */
    public OrdenInstalacionCerrada(String eventId, String ordenInstalacionId, String medidorId,
                                   String serial, String fabricante, String codigoPunto,
                                   String direccion, Instant cerradaEn) {
        this(eventId, ordenInstalacionId, medidorId, serial, fabricante, codigoPunto,
                direccion, cerradaEn, null, null, null, null, null);
    }
}
