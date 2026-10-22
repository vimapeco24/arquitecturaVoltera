package com.voltera.liquidacion.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.voltera.liquidacion.domain.model.LiquidacionCalculada;
import com.voltera.liquidacion.domain.port.out.LiquidacionEventPublisherPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.concurrent.TimeUnit;

/**
 * Adaptador de SALIDA (EDA): publica {@link LiquidacionCalculada} en el topic
 * 'topic.facturacion.liquidacion' del broker (Kafka/Redpanda). Es el eslabón
 * que cierra la orquestación por eventos: el blockchain-gateway consume ese
 * topic y ancla automáticamente (sin que el frontend llame al gateway).
 *
 * <p>Clave del mensaje = transaccionId (particiona por agregado, preserva orden
 * por transacción). Publicación confirmada con timeout corto: si el broker no
 * confirma devuelve {@code false}; el anclaje REST best-effort sigue de respaldo.</p>
 *
 * <p>Este bean SOLO se crea si {@code voltera.kafka.enabled=true} (ver
 * {@code MessagingConfiguration}); por defecto se usa el no-op.</p>
 */
public class KafkaLiquidacionEventPublisher implements LiquidacionEventPublisherPort {

    private static final Logger log = LoggerFactory.getLogger(KafkaLiquidacionEventPublisher.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final String topic;

    public KafkaLiquidacionEventPublisher(KafkaTemplate<String, String> kafkaTemplate, String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        this.topic = topic;
    }

    @Override
    public boolean publicar(LiquidacionCalculada evento) {
        try {
            String payload = objectMapper.writeValueAsString(evento);
            kafkaTemplate.send(topic, evento.transaccionId(), payload).get(3, TimeUnit.SECONDS);
            log.info("Evento LiquidacionCalculada publicado eventId={} transaccionId={} topic='{}'",
                    evento.eventId(), evento.transaccionId(), topic);
            return true;
        } catch (Exception ex) {
            log.warn("No se pudo publicar LiquidacionCalculada transaccionId={} (el anclaje REST queda de respaldo): {}",
                    evento.transaccionId(), ex.getMessage());
            return false;
        }
    }
}
