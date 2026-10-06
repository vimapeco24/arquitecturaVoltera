package com.voltera.notificaciones.domain.event;

/**
 * Evento de entrada (lamina 02): el BC Notificaciones consume
 * {@code MedidorSinReporte} para avisar al cliente que su medidor dejo de reportar.
 */
public record MedidorSinReporte(String medidorSerial, String ultimaLecturaEn) {
}
