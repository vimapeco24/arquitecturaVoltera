package com.voltera.ia.infrastructure.eventos;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Lazy;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consumidor Kafka (solo on-premise, voltera.kafka.enabled=true). {@code @Lazy(false)} porque el servicio
 * arranca con lazy-initialization y un listener perezoso nunca se registraría.
 */
@Component
@Lazy(false)
@ConditionalOnProperty(name = "voltera.kafka.enabled", havingValue = "true")
public class EventosKafkaConsumer {

    private final EventosEntrantesService servicio;

    public EventosKafkaConsumer(EventosEntrantesService servicio) {
        this.servicio = servicio;
    }

    @KafkaListener(topics = EventosEntrantesService.TOPIC_ALERTAS, groupId = "ia-service-alertas")
    public void alertas(String mensaje) {
        servicio.procesarAlerta(mensaje);
    }

    @KafkaListener(topics = EventosEntrantesService.TOPIC_CONOCIMIENTO, groupId = "ia-service-conocimiento")
    public void conocimiento(String mensaje) {
        servicio.procesarConocimiento(mensaje);
    }
}
