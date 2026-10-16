package com.voltera.notificaciones.infrastructure.rest.dto;

import com.voltera.notificaciones.domain.model.Notificacion;

import java.time.Instant;

/**
 * DTO de respuesta REST para una Notificacion.
 *
 * El modelo de dominio {@link Notificacion} usa getters sin prefijo 'get'
 * (estilo record) y campos private final, por lo que Jackson no puede
 * serializarlo directamente (daba 406 "No acceptable representation" / 500).
 * Este record expone las propiedades de forma estandar para la API REST,
 * igual que hacen el resto de servicios del proyecto.
 */
public record NotificacionResponse(
        String notificacionId,
        String medidorId,
        String tipo,
        String canal,
        String mensaje,
        Instant generadaEn
) {
    public static NotificacionResponse desde(Notificacion n) {
        return new NotificacionResponse(
                n.notificacionId(),
                n.medidorId(),
                n.tipo() != null ? n.tipo().name() : null,
                n.canal() != null ? n.canal().name() : null,
                n.mensaje(),
                n.generadaEn()
        );
    }
}
