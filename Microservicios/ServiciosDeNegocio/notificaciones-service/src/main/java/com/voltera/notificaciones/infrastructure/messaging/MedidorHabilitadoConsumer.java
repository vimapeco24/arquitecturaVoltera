package com.voltera.notificaciones.infrastructure.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.voltera.notificaciones.domain.event.MedidorHabilitado;
import com.voltera.notificaciones.domain.port.in.NotificarClienteUseCase;
import com.voltera.notificaciones.domain.port.out.InboxPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consume {@code medidores-habilitacion}: ante MedidorHabilitado (trae 'estado')
 * notifica al cliente. Ignora MedidorActivado/HabilitacionFallida/MedidorSuspendido.
 * Idempotente por tabla inbox (lamina 10).
 */
@Component
@Lazy(false)
public class MedidorHabilitadoConsumer {

    private static final Logger log = LoggerFactory.getLogger(MedidorHabilitadoConsumer.class);
    private static final String CONSUMIDOR = "notificaciones-consumer#medidores-habilitacion";

    private final NotificarClienteUseCase useCase;
    private final InboxPort inbox;
    private final ObjectMapper objectMapper;

    public MedidorHabilitadoConsumer(NotificarClienteUseCase useCase, InboxPort inbox,
                                     ObjectMapper objectMapper) {
        this.useCase = useCase;
        this.inbox = inbox;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "medidores-habilitacion", groupId = "notificaciones-consumer")
    public void escuchar(String mensaje) {
        try {
            String t = mensaje == null ? "" : mensaje.trim();
            if (!t.startsWith("{")) return;
            JsonNode n = objectMapper.readTree(t);
            // Solo MedidorHabilitado trae 'estado' (event-carried state transfer).
            if (!n.hasNonNull("estado")) return;
            if (!inbox.registrarSiEsNuevo(CONSUMIDOR,
                    ClaveIdempotencia.derivar(n, "MedidorHabilitado"), "MedidorHabilitado")) {
                log.info("MedidorHabilitado duplicado ignorado en notificaciones.");
                return;
            }
            String medidorId = n.hasNonNull("medidorId") ? n.get("medidorId").asText() : null;
            String serial = n.hasNonNull("serial") ? n.get("serial").asText() : null;
            useCase.notificarMedidorHabilitado(
                    new MedidorHabilitado(medidorId, serial, n.get("estado").asText()));
            log.info("Notificacion por MedidorHabilitado. medidorId={} serial={}", medidorId, serial);
        } catch (Exception e) {
            log.error("Error procesando MedidorHabilitado. payload={}", mensaje, e);
        }
    }
}
