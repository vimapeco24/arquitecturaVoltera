package com.voltera.ia.infrastructure.messaging;

import com.voltera.ia.domain.model.IaRecomendacionGenerada;
import com.voltera.ia.domain.port.out.EventPublisherPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Publicador de eventos para modo NUBE (PaaS). Cuando voltera.kafka.enabled no es
 * "true" (el broker Redpanda vive en la LAN privada del clúster y no es
 * alcanzable desde la nube), el evento IaRecomendacionGenerada se registra en el
 * log en lugar de enviarse al broker. Así el copiloto funciona igual del "otro
 * lado" sin depender del servidor on-premise saturado.
 *
 * <p>matchIfMissing=true: este bean es el publicador por defecto si no se activa
 * Kafka explicitamente.</p>
 */
@Component
@ConditionalOnProperty(name = "voltera.kafka.enabled", havingValue = "false", matchIfMissing = true)
public class LogEventPublisher implements EventPublisherPort {

    private static final Logger log = LoggerFactory.getLogger(LogEventPublisher.class);

    @Override
    public boolean publicar(IaRecomendacionGenerada evento) {
        log.info("[modo-nube/log] IaRecomendacionGenerada eventId={} siniestroId={} decision={} "
                        + "requiereRevisionHumana={} motor={} (broker no disponible; evento no enviado a Kafka)",
                evento.eventId(), evento.siniestroId(), evento.decisionPropuesta(),
                evento.requiereRevisionHumana(), evento.motor());
        return false;
    }
}
