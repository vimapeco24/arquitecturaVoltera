package com.voltera.blockchain.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.voltera.blockchain.domain.model.LiquidacionAnclada;
import com.voltera.blockchain.domain.port.out.EventPublisherPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.concurrent.TimeUnit;

/**
 * Adaptador de SALIDA (EDA): publica LiquidacionAnclada en el topic
 * 'topic.blockchain.anclada' de Redpanda/Kafka. Clave = transaccionId
 * (particionamiento por agregado). Publicacion confirmada con timeout corto.
 */
public class KafkaEventPublisher implements EventPublisherPort {

    private static final Logger log = LoggerFactory.getLogger(KafkaEventPublisher.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final String topic;

    public KafkaEventPublisher(KafkaTemplate<String, String> kafkaTemplate, String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        this.topic = topic;
    }

    @Override
    public boolean publicar(LiquidacionAnclada evento) {
        try {
            String payload = objectMapper.writeValueAsString(evento);
            kafkaTemplate.send(topic, evento.transaccionId(), payload).get(3, TimeUnit.SECONDS);
            log.info("Evento LiquidacionAnclada publicado eventId={} transaccion={} topic='{}'",
                    evento.eventId(), evento.transaccionId(), topic);
            return true;
        } catch (Exception ex) {
            log.warn("No se pudo publicar LiquidacionAnclada transaccion={}: {}",
                    evento.transaccionId(), ex.getMessage());
            return false;
        }
    }
}
