package com.voltera.ia.infrastructure.messaging;

import com.voltera.ia.domain.model.gobernanza.EventoGobernanza;
import com.voltera.ia.domain.port.out.GobernanzaEventPublisherPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Publicador del tópico {@code der.gobernanza} para modo NUBE (PaaS). Cuando
 * voltera.kafka.enabled no es "true", el broker no es alcanzable y el evento se
 * registra en el log en lugar de enviarse a Kafka.
 */
@Component
@ConditionalOnProperty(name = "voltera.kafka.enabled", havingValue = "false", matchIfMissing = true)
public class LogGobernanzaPublisher implements GobernanzaEventPublisherPort {

    private static final Logger log = LoggerFactory.getLogger(LogGobernanzaPublisher.class);

    @Override
    public boolean publicar(EventoGobernanza e) {
        log.info("[modo-nube/log] der.gobernanza tipo={} propuesta={} activo={} estado={} firmas={} txRef={} "
                        + "(broker no disponible; no enviado a Kafka)",
                e.tipo(), e.propuestaId(), e.activoId(), e.estado(), e.firmas(), e.txRef());
        return false;
    }
}
