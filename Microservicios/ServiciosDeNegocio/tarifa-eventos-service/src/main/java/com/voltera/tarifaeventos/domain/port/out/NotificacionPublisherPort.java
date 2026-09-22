package com.voltera.tarifaeventos.domain.port.out;

import com.voltera.tarifaeventos.domain.model.NotificacionLiquidada;

/**
 * Puerto de SALIDA: publica una notificacion en el <b>topico de notificacion</b>
 * del broker (Kafka/Redpanda), para que otros servicios (correo, app, portal)
 * reaccionen. Implementado en infraestructura por un adaptador KafkaTemplate.
 */
public interface NotificacionPublisherPort {
    void publicar(NotificacionLiquidada notificacion);
}
