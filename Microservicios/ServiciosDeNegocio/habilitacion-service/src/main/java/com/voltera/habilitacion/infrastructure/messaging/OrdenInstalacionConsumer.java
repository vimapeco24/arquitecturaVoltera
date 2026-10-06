package com.voltera.habilitacion.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.voltera.habilitacion.domain.event.OrdenInstalacionCerrada;
import com.voltera.habilitacion.domain.port.in.HabilitarMedidorUseCase;
import com.voltera.habilitacion.domain.port.out.InboxPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consumidor del topic {@code ordenes-instalacion} (evento externo
 * OrdenInstalacionCerrada). Dispara el alta del medidor de forma idempotente
 * (tabla inbox): una reentrega del mismo eventId no vuelve a habilitar.
 */
@Component
@Lazy(false)
public class OrdenInstalacionConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrdenInstalacionConsumer.class);
    private static final String CONSUMIDOR = "habilitacion-consumer";

    private final HabilitarMedidorUseCase useCase;
    private final InboxPort inbox;
    private final ObjectMapper objectMapper;

    public OrdenInstalacionConsumer(HabilitarMedidorUseCase useCase, InboxPort inbox,
                                    ObjectMapper objectMapper) {
        this.useCase = useCase;
        this.inbox = inbox;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "ordenes-instalacion", groupId = CONSUMIDOR)
    public void escuchar(String mensaje) {
        try {
            OrdenInstalacionCerrada orden = objectMapper.readValue(mensaje, OrdenInstalacionCerrada.class);
            if (!inbox.registrarSiEsNuevo(CONSUMIDOR, orden.eventId(), "OrdenInstalacionCerrada")) {
                log.info("OrdenInstalacionCerrada duplicada ignorada. eventId={}", orden.eventId());
                return;
            }
            var medidor = useCase.habilitarDesdeOrden(orden);
            log.info("Medidor habilitado desde orden. medidorId={}, estado={}",
                    medidor.medidorId(), medidor.estado());
        } catch (Exception e) {
            log.error("Error procesando OrdenInstalacionCerrada. payload={}", mensaje, e);
        }
    }
}
