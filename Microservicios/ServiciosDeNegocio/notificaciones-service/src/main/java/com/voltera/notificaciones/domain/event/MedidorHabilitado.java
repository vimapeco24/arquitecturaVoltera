package com.voltera.notificaciones.domain.event;

/**
 * Evento de entrada (lamina 02): el BC Notificaciones consume
 * {@code MedidorHabilitado} para avisar al cliente que su medidor quedo activo.
 */
public record MedidorHabilitado(String medidorId, String serial, String estado) {
}
