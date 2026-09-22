package com.voltera.telemetria.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.voltera.telemetria.domain.model.ConsumoRegistrado;
import com.voltera.telemetria.domain.port.out.EventPublisherPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Adaptador de SALIDA (EDA): publica el evento ConsumoRegistrado en el topic
 * 'consumo-registrado' del broker (Kafka/Redpanda) usando KafkaTemplate.
 *
 * <p>La clave del mensaje es el medidorId (particionamiento por agregado: todas
 * las lecturas de un mismo medidor conservan orden dentro de su particion).</p>
 *
 * <p>A diferencia de un publisher best-effort, este devuelve {@code boolean}:
 * espera la confirmacion del broker (con timeout corto). Si falla, devuelve
 * {@code false} para que el servicio de aplicacion active el buffer offline
 * (store-and-forward).</p>
 */
@Component
public class KafkaEventPublisher implements EventPublisherPort {

    private static final Logger log = LoggerFactory.getLogger(KafkaEventPublisher.class);
    private static final String TOPIC = "consumo-registrado";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public KafkaEventPublisher(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    }

    @Override
    public boolean publicar(ConsumoRegistrado evento) {
        try {
            String payload = objectMapper.writeValueAsString(evento);
            // get(...) con timeout: si el broker no confirma, lanzamos y devolvemos false.
            kafkaTemplate.send(TOPIC, evento.medidorId(), payload).get(3, TimeUnit.SECONDS);
            log.info("Evento ConsumoRegistrado publicado eventId={} medidorId={} extra={} topic='{}'",
                    evento.eventId(), evento.medidorId(), evento.consumoExtra(), TOPIC);
            return true;
        } catch (Exception ex) {
            log.warn("No se pudo publicar ConsumoRegistrado eventId={} medidorId={} (se ira al buffer offline): {}",
                    evento.eventId(), evento.medidorId(), ex.getMessage());
            return false;
        }
    }
}
