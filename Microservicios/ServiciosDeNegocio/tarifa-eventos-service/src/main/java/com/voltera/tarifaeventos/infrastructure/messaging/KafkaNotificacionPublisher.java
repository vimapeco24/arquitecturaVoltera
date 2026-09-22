package com.voltera.tarifaeventos.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.voltera.tarifaeventos.domain.model.NotificacionLiquidada;
import com.voltera.tarifaeventos.domain.port.out.NotificacionPublisherPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Adaptador de SALIDA: publica la NotificacionLiquidada en el <b>topico de
 * notificacion</b> 'notificacion-liquidada' del broker (Kafka/Redpanda).
 *
 * <p>Es best-effort: si el broker no responde, se loguea y se continua (la
 * notificacion no bloquea la activacion del medidor).</p>
 */
@Component
public class KafkaNotificacionPublisher implements NotificacionPublisherPort {

    private static final Logger log = LoggerFactory.getLogger(KafkaNotificacionPublisher.class);
    private static final String TOPIC = "notificacion-liquidada";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public KafkaNotificacionPublisher(KafkaTemplate<String, String> kafkaTemplate,
                                      ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void publicar(NotificacionLiquidada notificacion) {
        try {
            String payload = objectMapper.writeValueAsString(notificacion);
            kafkaTemplate.send(TOPIC, notificacion.medidorId(), payload)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.warn("No se pudo publicar NotificacionLiquidada id={} medidorId={} en topic '{}': {}",
                                    notificacion.notificacionId(), notificacion.medidorId(), TOPIC, ex.getMessage());
                        } else {
                            log.info("NotificacionLiquidada publicada id={} medidorId={} topic='{}'",
                                    notificacion.notificacionId(), notificacion.medidorId(), TOPIC);
                        }
                    });
        } catch (Exception ex) {
            log.warn("Fallo al serializar/publicar NotificacionLiquidada id={}: {}",
                    notificacion.notificacionId(), ex.getMessage());
        }
    }
}
