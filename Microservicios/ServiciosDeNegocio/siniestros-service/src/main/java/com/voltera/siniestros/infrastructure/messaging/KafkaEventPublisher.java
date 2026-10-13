package com.voltera.siniestros.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.voltera.siniestros.domain.model.SiniestroAprobado;
import com.voltera.siniestros.domain.port.out.EventPublisherPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Adaptador de SALIDA (EDA): publica el evento SiniestroAprobado en el topic
 * 'topic.siniestros.aprobado' del broker (Kafka/Redpanda) usando KafkaTemplate.
 *
 * <p>La clave del mensaje es el siniestroId (particionamiento por agregado,
 * preserva el orden por siniestro). La publicacion es <b>confirmada</b>: espera
 * el ack del broker con un timeout corto; si el broker no confirma, devuelve
 * {@code false} para que el servicio de aplicacion encole el evento en el buffer
 * offline (store-and-forward) y asi no se pierda ninguna aprobacion.</p>
 */
@Component
public class KafkaEventPublisher implements EventPublisherPort {

    private static final Logger log = LoggerFactory.getLogger(KafkaEventPublisher.class);
    private static final String TOPIC = "topic.siniestros.aprobado";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public KafkaEventPublisher(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    }

    @Override
    public boolean publicar(SiniestroAprobado evento) {
        try {
            String payload = objectMapper.writeValueAsString(evento);
            // get(...) con timeout: si el broker no confirma, lanzamos y devolvemos false.
            kafkaTemplate.send(TOPIC, evento.siniestroId(), payload).get(3, TimeUnit.SECONDS);
            log.info("Evento SiniestroAprobado publicado eventId={} siniestroId={} topic='{}'",
                    evento.eventId(), evento.siniestroId(), TOPIC);
            return true;
        } catch (Exception ex) {
            log.warn("No se pudo publicar SiniestroAprobado eventId={} siniestroId={} (se ira al buffer offline): {}",
                    evento.eventId(), evento.siniestroId(), ex.getMessage());
            return false;
        }
    }
}
