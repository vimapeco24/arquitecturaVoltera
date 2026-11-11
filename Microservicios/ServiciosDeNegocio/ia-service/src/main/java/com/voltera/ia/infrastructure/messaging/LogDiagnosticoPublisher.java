package com.voltera.ia.infrastructure.messaging;

import com.voltera.ia.domain.model.der.DiagnosticoDerGenerado;
import com.voltera.ia.domain.port.out.DiagnosticoEventPublisherPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Publicador del evento DiagnosticoDerGenerado para modo NUBE (PaaS). Cuando
 * voltera.kafka.enabled no es "true", el broker no es alcanzable y el evento se
 * registra en el log en lugar de enviarse a Kafka.
 */
@Component
@ConditionalOnProperty(name = "voltera.kafka.enabled", havingValue = "false", matchIfMissing = true)
public class LogDiagnosticoPublisher implements DiagnosticoEventPublisherPort {

    private static final Logger log = LoggerFactory.getLogger(LogDiagnosticoPublisher.class);

    @Override
    public boolean publicar(DiagnosticoDerGenerado evento) {
        log.info("[modo-nube/log] DiagnosticoDerGenerado eventId={} activo={} tipo={} severidad={} "
                        + "accion={} requiereAprobacionHumana={} motor={} (broker no disponible; no enviado a Kafka)",
                evento.eventId(), evento.activoId(), evento.tipoActivo(), evento.severidad(),
                evento.accionPropuesta(), evento.requiereAprobacionHumana(), evento.motor());
        return false;
    }
}
