package com.voltera.liquidacionmensual.infrastructure.scheduling;

import com.voltera.liquidacionmensual.domain.model.MensajeOutbox;
import com.voltera.liquidacionmensual.domain.port.out.OutboxPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Relay del OUTBOX de Liquidacion Mensual: publica {@code LiquidacionCalculada}
 * al topic {@code liquidaciones-calculadas} (destino: Facturacion).
 */
@Component
public class OutboxRelay {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelay.class);
    private static final String TOPIC = "liquidaciones-calculadas";

    private final OutboxPort outbox;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public OutboxRelay(OutboxPort outbox, KafkaTemplate<String, String> kafkaTemplate) {
        this.outbox = outbox;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Scheduled(fixedDelayString = "${liquidacion.outbox.relay-interval-ms:3000}")
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
