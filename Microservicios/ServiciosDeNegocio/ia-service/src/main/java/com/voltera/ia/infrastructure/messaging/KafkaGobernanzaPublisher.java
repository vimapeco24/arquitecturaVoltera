package com.voltera.ia.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.voltera.ia.domain.model.gobernanza.EventoGobernanza;
import com.voltera.ia.domain.port.out.GobernanzaEventPublisherPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Productor de los eventos de gobernanza hacia el tópico {@code der.gobernanza}
 * (PropuestaAccionCreada, FirmaRegistrada, AccionAprobada/Rechazada/Ejecutada,
 * FirmaVencida). Solo activo on-premise (voltera.kafka.enabled=true). En la nube
 * se usa {@link LogGobernanzaPublisher}.
 */
@Component
@ConditionalOnProperty(name = "voltera.kafka.enabled", havingValue = "true")
public class KafkaGobernanzaPublisher implements GobernanzaEventPublisherPort {

    private static final Logger log = LoggerFactory.getLogger(KafkaGobernanzaPublisher.class);
    private static final String TOPIC = "der.gobernanza";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public KafkaGobernanzaPublisher(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    }

    @Override
    public boolean publicar(EventoGobernanza evento) {
        try {
            String payload = objectMapper.writeValueAsString(evento);
            kafkaTemplate.send(TOPIC, evento.activoId(), payload).get(3, TimeUnit.SECONDS);
            log.info("Evento {} publicado propuesta={} activo={} estado={} topic='{}'",
                    evento.tipo(), evento.propuestaId(), evento.activoId(), evento.estado(), TOPIC);
            return true;
        } catch (Exception ex) {
            log.warn("No se pudo publicar {} propuesta={}: {}",
                    evento.tipo(), evento.propuestaId(), ex.getMessage());
            return false;
        }
    }
}
