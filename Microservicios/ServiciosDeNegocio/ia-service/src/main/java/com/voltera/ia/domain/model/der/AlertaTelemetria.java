package com.voltera.ia.domain.model.der;

import java.math.BigDecimal;

/**
 * Señal de entrada al Agente-Monitor: una alerta de telemetría de un activo DER
 * (p. ej. degradación de batería, inversor fuera de rango, medidor sin señal).
 *
 * @param activoId        identificador del activo (panel/batería/inversor/EV).
 * @param tipoActivo      clase del activo.
 * @param comunidadId     comunidad energética a la que pertenece.
 * @param metrica         métrica observada (p. ej. "voltaje", "SoH", "latencia").
 * @param valor           valor observado de la métrica.
 * @param umbral          umbral esperado para esa métrica.
 * @param minutosSinSenal minutos sin reportar (0 si reporta normal).
 * @param descripcion     texto libre del operador o del head-end.
 * @param usuario         identidad del operador que consulta (IAM: mínimo privilegio).
 */
public record AlertaTelemetria(
        String activoId,
        TipoActivo tipoActivo,
        String comunidadId,
        String metrica,
        BigDecimal valor,
        BigDecimal umbral,
        int minutosSinSenal,
        String descripcion,
        String usuario
) {
    public AlertaTelemetria {
        if (valor == null) valor = BigDecimal.ZERO;
        if (umbral == null) umbral = BigDecimal.ZERO;
        if (descripcion == null) descripcion = "";
    }

    /** true si la métrica observada está fuera del umbral esperado. */
    public boolean fueraDeRango() {
        return valor.compareTo(umbral) > 0;
    }

    /** true si el activo lleva demasiado tiempo sin reportar (medidor sin señal). */
    public boolean sinSenal() {
        return minutosSinSenal >= 15;
    }
}
