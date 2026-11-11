package com.voltera.ia.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.voltera.ia.domain.model.IaRecomendacionGenerada;
import com.voltera.ia.domain.port.out.EventPublisherPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Productor del evento IaRecomendacionGenerada hacia el broker (Kafka/Redpanda).
 * Solo activo en modo on-premise (voltera.kafka.enabled=true). En modo NUBE el
 * broker no es alcanzable y en su lugar se usa {@link LogEventPublisher}.
 *
 * <p>Best-effort: si el broker no confirma dentro del timeout, se registra y se
 * devuelve false sin romper el flujo del copiloto.</p>
 */
@Component
@ConditionalOnProperty(name = "voltera.kafka.enabled", havingValue = "true")
public class KafkaEventPublisher implements EventPublisherPort {

    private static final Logger log = LoggerFactory.getLogger(KafkaEventPublisher.class);
    private static final String TOPIC = "topic.ia.recomendacion";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public KafkaEventPublisher(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    }

    @Override
    public boolean publicar(IaRecomendacionGenerada evento) {
        try {
            String payload = objectMapper.writeValueAsString(evento);
            String key = evento.siniestroId() != null ? evento.siniestroId() : evento.recomendacionId().toString();
            kafkaTemplate.send(TOPIC, key, payload).get(3, TimeUnit.SECONDS);
            log.info("Evento IaRecomendacionGenerada publicado eventId={} siniestroId={} decision={} topic='{}'",
                    evento.eventId(), evento.siniestroId(), evento.decisionPropuesta(), TOPIC);
            return true;
        } catch (Exception ex) {
            log.warn("No se pudo publicar IaRecomendacionGenerada eventId={} siniestroId={}: {}",
                    evento.eventId(), evento.siniestroId(), ex.getMessage());
            return false;
        }
    }
}
