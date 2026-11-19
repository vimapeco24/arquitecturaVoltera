package com.voltera.liquidacion.infrastructure.scheduling;

import com.voltera.liquidacion.domain.model.MensajeOutbox;
import com.voltera.liquidacion.domain.port.out.OutboxPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Relay del OUTBOX de Liquidacion P2P: publica {@code LiquidacionCalculada} al
 * topic {@code topic.facturacion.liquidacion}. Solo existe con
 * {@code voltera.kafka.enabled=true}.
 */
@Component
@ConditionalOnProperty(name = "voltera.kafka.enabled", havingValue = "true")
public class OutboxRelay {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelay.class);

    private final OutboxPort outbox;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final String topic;

    public OutboxRelay(OutboxPort outbox,
                       KafkaTemplate<String, String> kafkaTemplate,
                       @Value("${voltera.eventos.topic-liquidacion:topic.facturacion.liquidacion}") String topic) {
        this.outbox = outbox;
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    @Scheduled(fixedDelayString = "${voltera.outbox.relay-interval-ms:3000}")
    public void publicarPendientes() {
        for (MensajeOutbox m : outbox.pendientes()) {
            try {
                kafkaTemplate.send(topic, m.clave(), m.payloadJson()).get();
                outbox.marcarPublicado(m.id());
                log.info("Outbox -> broker. tipo={} clave={} id={}", m.tipoEvento(), m.clave(), m.id());
            } catch (Exception e) {
                log.warn("No se pudo publicar outbox id={} (reintento luego): {}", m.id(), e.getMessage());
                break;
            }
        }
    }
}
