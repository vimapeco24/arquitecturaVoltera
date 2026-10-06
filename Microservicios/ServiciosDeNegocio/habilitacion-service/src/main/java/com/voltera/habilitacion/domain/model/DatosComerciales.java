package com.voltera.habilitacion.domain.model;

import java.util.List;

/**
 * Value Object con los datos que viajan en {@code MedidorHabilitado} como parte del
 * <b>Event-Carried State Transfer</b> (lámina 03): "MedidorHabilitado lleva cliente,
 * proveedor, protocolo, punto de medición, plan y canales. Cada consumidor guarda su
 * réplica local y no vuelve a preguntar."
 *
 * <p>Agrupa los atributos comerciales/técnicos del alta que no pertenecen a la
 * identidad del dispositivo ni al punto de medición:</p>
 * <ul>
 *   <li>{@code clienteId} — titular del servicio (lo consumen Tarifas y Notificaciones).</li>
 *   <li>{@code proveedorAmi} — fabricante/operador del head-end (lo consume Integración AMI).</li>
 *   <li>{@code protocolo} — protocolo de comunicación del medidor (p.ej. MQTT, DLMS).</li>
 *   <li>{@code plan} — plan tarifario inicial (lo consume Tarifas).</li>
 *   <li>{@code canales} — canales de notificación preferidos del cliente (los consume Notificaciones).</li>
 * </ul>
 */
public record DatosComerciales(
        String clienteId,
        String proveedorAmi,
        String protocolo,
        String plan,
        List<String> canales) {

    public DatosComerciales {
        // Valores por defecto seguros: el alta nunca debe fallar por datos comerciales ausentes.
        if (clienteId == null || clienteId.isBlank()) clienteId = "SIN_CLIENTE";
        if (proveedorAmi == null || proveedorAmi.isBlank()) proveedorAmi = "DESCONOCIDO";
        if (protocolo == null || protocolo.isBlank()) protocolo = "MQTT";
        if (plan == null || plan.isBlank()) plan = "RESIDENCIAL_BASICO";
        canales = (canales == null || canales.isEmpty())
                ? List.of("EMAIL")
                : List.copyOf(canales);
    }

    /** Datos comerciales por defecto cuando la orden no los trae (compatibilidad hacia atrás). */
    public static DatosComerciales porDefecto() {
        return new DatosComerciales(null, null, null, null, null);
    }

    /** Representación de canales como cadena separada por comas para el payload del evento. */
    public String canalesComoTexto() {
        return String.join(",", canales);
    }
}
