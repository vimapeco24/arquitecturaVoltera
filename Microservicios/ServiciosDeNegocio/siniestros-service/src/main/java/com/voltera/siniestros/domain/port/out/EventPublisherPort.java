package com.voltera.siniestros.domain.port.out;

import com.voltera.siniestros.domain.model.SiniestroAprobado;

/**
 * Puerto de SALIDA: publica eventos de dominio hacia el broker (Kafka/Redpanda).
 * Implementado en infraestructura por un adaptador basado en KafkaTemplate.
 */
public interface EventPublisherPort {
    void publicar(SiniestroAprobado evento);
}
