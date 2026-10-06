package com.voltera.ingesta.infrastructure.scheduling;

import com.voltera.ingesta.domain.model.MensajeOutbox;
import com.voltera.ingesta.domain.port.out.OutboxPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Relay del OUTBOX de Ingesta.
 *
 * <p>Publica LecturaValidada / LecturaSospechosaDetectada / CanalIngestaCreado al
 * topic principal {@code telemetria-lecturas-validadas} (clave = medidorSerial para
 * preservar el orden por medidor), y además enruta las ALERTAS al topic dedicado
 * {@code telemetria-alertas} (lámina 03 · CQRS: "telemetria.alertas · publican
 * Ingesta y Core"). Hoy la única alerta de Ingesta es
 * {@code LecturaSospechosaDetectada}.</p>
 *
 * <p>La alerta se publica en AMBOS topics: en el principal para no romper a los
 * consumidores existentes y en el de alertas para el consumidor dedicado.</p>
 */
@Component
public class OutboxRelay {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelay.class);
    private static final String TOPIC = "telemetria-lecturas-validadas";
    private static final String TOPIC_ALERTAS = "telemetria-alertas";

    private final OutboxPort outbox;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public OutboxRelay(OutboxPort outbox, KafkaTemplate<String, String> kafkaTemplate) {
        this.outbox = outbox;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Scheduled(fixedDelayString = "${ingesta.outbox.relay-interval-ms:3000}")
    public void publicarPendientes() {
        for (MensajeOutbox m : outbox.pendientes()) {
            try {
                kafkaTemplate.send(TOPIC, m.clave(), m.payloadJson()).get();
                if (esAlerta(m.tipoEvento())) {
                    kafkaTemplate.send(TOPIC_ALERTAS, m.clave(), m.payloadJson()).get();
                }
                outbox.marcarPublicado(m.id());
                log.info("Outbox -> broker. tipo={} clave={} id={}", m.tipoEvento(), m.clave(), m.id());
            } catch (Exception e) {
                log.warn("No se pudo publicar outbox id={} (reintento luego): {}", m.id(), e.getMessage());
                break;
            }
        }
    }

    /** Eventos de alerta que además se publican en {@code telemetria-alertas}. */
    private boolean esAlerta(String tipoEvento) {
        return "LecturaSospechosaDetectada".equals(tipoEvento);
    }
}
