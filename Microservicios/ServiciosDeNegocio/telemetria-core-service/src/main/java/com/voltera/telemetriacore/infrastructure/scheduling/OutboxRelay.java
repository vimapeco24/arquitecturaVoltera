package com.voltera.telemetriacore.infrastructure.scheduling;

import com.voltera.telemetriacore.domain.model.MensajeOutbox;
import com.voltera.telemetriacore.domain.port.out.OutboxPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Relay del OUTBOX de Telemetria Core: publica ConsumoIntervaloRegistrado y
 * MedidorSinReporte al topic {@code telemetria-consumo-intervalos}.
 */
@Component
public class OutboxRelay {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelay.class);
    private static final String TOPIC = "telemetria-consumo-intervalos";

    private final OutboxPort outbox;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public OutboxRelay(OutboxPort outbox, KafkaTemplate<String, String> kafkaTemplate) {
        this.outbox = outbox;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Scheduled(fixedDelayString = "${telemetria-core.outbox.relay-interval-ms:3000}")
    public void publicarPendientes() {
        for (MensajeOutbox m : outbox.pendientes()) {
            try {
                kafkaTemplate.send(TOPIC, m.clave(), m.payloadJson()).get();
                outbox.marcarPublicado(m.id());
                log.info("Outbox -> broker. tipo={} clave={} id={}", m.tipoEvento(), m.clave(), m.id());
            } catch (Exception e) {
                log.warn("No se pudo publicar outbox id={} (reintento luego): {}", m.id(), e.getMessage());
                break;
            }
        }
    }
}
