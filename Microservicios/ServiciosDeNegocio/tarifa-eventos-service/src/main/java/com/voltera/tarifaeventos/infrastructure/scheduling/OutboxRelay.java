package com.voltera.tarifaeventos.infrastructure.scheduling;

import com.voltera.tarifaeventos.domain.model.MensajeOutbox;
import com.voltera.tarifaeventos.domain.port.out.OutboxPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Relay del patron OUTBOX transaccional (lamina 10).
 *
 * <p>Cada pocos segundos lee las filas no publicadas del outbox y las envia al
 * broker (Kafka/Redpanda) en el topic 'medidores-habilitacion' usando la clave de
 * particion (medidorId) para preservar el orden por medidor. Solo tras un envio
 * exitoso marca la fila como publicada; si el broker esta caido, reintenta en el
 * siguiente ciclo (entrega al-menos-una-vez; los consumidores deduplican via inbox).</p>
 */
@Component
public class OutboxRelay {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelay.class);
    private static final String TOPIC = "medidores-habilitacion";

    private final OutboxPort outbox;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public OutboxRelay(OutboxPort outbox, KafkaTemplate<String, String> kafkaTemplate) {
        this.outbox = outbox;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Scheduled(fixedDelayString = "${orquestacion.outbox.relay-interval-ms:3000}")
    public void publicarPendientes() {
        for (MensajeOutbox m : outbox.pendientes()) {
            try {
                kafkaTemplate.send(TOPIC, m.clave(), m.payloadJson()).get();
                outbox.marcarPublicado(m.id());
                log.info("Outbox -> broker. tipo={} clave={} outboxId={}", m.tipoEvento(), m.clave(), m.id());
            } catch (Exception e) {
                // Broker caido: paramos; reintentaremos las pendientes en el proximo ciclo.
                log.warn("No se pudo publicar outbox id={} (reintento luego): {}", m.id(), e.getMessage());
                break;
            }
        }
    }
}
