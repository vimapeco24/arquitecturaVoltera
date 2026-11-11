package com.voltera.ia.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.voltera.ia.domain.model.der.DiagnosticoDerGenerado;
import com.voltera.ia.domain.port.out.DiagnosticoEventPublisherPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Productor del evento DiagnosticoDerGenerado hacia el broker (topic.ia.diagnostico).
 * Solo activo on-premise (voltera.kafka.enabled=true). En la nube se usa
 * {@link LogDiagnosticoPublisher}.
 */
@Component
@ConditionalOnProperty(name = "voltera.kafka.enabled", havingValue = "true")
public class KafkaDiagnosticoPublisher implements DiagnosticoEventPublisherPort {

    private static final Logger log = LoggerFactory.getLogger(KafkaDiagnosticoPublisher.class);
    private static final String TOPIC = "topic.ia.diagnostico";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public KafkaDiagnosticoPublisher(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    }

    @Override
    public boolean publicar(DiagnosticoDerGenerado evento) {
        try {
            String payload = objectMapper.writeValueAsString(evento);
            kafkaTemplate.send(TOPIC, evento.activoId(), payload).get(3, TimeUnit.SECONDS);
            log.info("Evento DiagnosticoDerGenerado publicado eventId={} activo={} severidad={} accion={} topic='{}'",
                    evento.eventId(), evento.activoId(), evento.severidad(), evento.accionPropuesta(), TOPIC);
            return true;
        } catch (Exception ex) {
            log.warn("No se pudo publicar DiagnosticoDerGenerado eventId={} activo={}: {}",
                    evento.eventId(), evento.activoId(), ex.getMessage());
            return false;
        }
    }
}
