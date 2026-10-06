package com.voltera.notificaciones.domain.event;

/**
 * Evento de entrada (lamina 02): el BC Notificaciones consume
 * {@code LecturaSospechosaDetectada} para alertar al cliente de un consumo inusual.
 */
public record LecturaSospechosaDetectada(String medidorSerial, double consumoKwh, String motivo) {
}
