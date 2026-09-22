package com.voltera.siniestros.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.voltera.siniestros.domain.model.SiniestroAprobado;
import com.voltera.siniestros.domain.port.out.EventPublisherPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Adaptador de SALIDA (EDA): publica el evento SiniestroAprobado en el topic
 * 'siniestro-aprobado' del broker (Kafka/Redpanda) usando KafkaTemplate.
 *
 * La clave del mensaje es el siniestroId (para particionamiento por agregado).
 * Si el broker no esta disponible, se loguea un warning y el servicio continua
 * (la publicacion es best-effort y no rompe la transaccion de negocio).
 */
@Component
public class KafkaEventPublisher implements EventPublisherPort {

    private static final Logger log = LoggerFactory.getLogger(KafkaEventPublisher.class);
    private static final String TOPIC = "siniestro-aprobado";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public KafkaEventPublisher(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    }

    @Override
    public void publicar(SiniestroAprobado evento) {
        try {
            String payload = objectMapper.writeValueAsString(evento);
            kafkaTemplate.send(TOPIC, evento.siniestroId(), payload)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.warn("No se pudo publicar SiniestroAprobado eventId={} siniestroId={} en topic '{}': {}",
                                    evento.eventId(), evento.siniestroId(), TOPIC, ex.getMessage());
                        } else {
                            log.info("Evento SiniestroAprobado publicado eventId={} siniestroId={} topic='{}'",
                                    evento.eventId(), evento.siniestroId(), TOPIC);
                        }
                    });
        } catch (Exception ex) {
            log.warn("Fallo al serializar/publicar SiniestroAprobado eventId={} siniestroId={}: {}",
                    evento.eventId(), evento.siniestroId(), ex.getMessage());
        }
    }
}
