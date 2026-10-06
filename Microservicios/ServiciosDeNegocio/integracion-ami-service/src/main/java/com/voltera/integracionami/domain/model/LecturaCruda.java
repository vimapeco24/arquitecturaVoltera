package com.voltera.integracionami.domain.model;

import java.time.Instant;

/**
 * Value Object: una lectura cruda tal como la entrega el head-end del proveedor,
 * ANTES de traducirla a formato canonico.
 *
 * @param medidorSerial serial del medidor segun el proveedor
 * @param valor         valor crudo (p. ej. registro acumulado del medidor)
 * @param unidad        unidad reportada por el proveedor (p. ej. "Wh", "kWh")
 * @param capturadaEn   timestamp de captura del proveedor
 */
public record LecturaCruda(String medidorSerial, double valor, String unidad, Instant capturadaEn) {
    public LecturaCruda {
        if (medidorSerial == null || medidorSerial.isBlank()) {
            throw new IllegalArgumentException("medidorSerial es obligatorio");
        }
        if (capturadaEn == null) {
            capturadaEn = Instant.now();
        }
    }
}
