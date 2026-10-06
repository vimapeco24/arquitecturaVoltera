package com.voltera.notificaciones.infrastructure.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.voltera.notificaciones.domain.event.FacturaEmitida;
import com.voltera.notificaciones.domain.port.in.NotificarClienteUseCase;
import com.voltera.notificaciones.domain.port.out.InboxPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consume {@code facturas-emitidas}: ante FacturaEmitida (de Facturacion) notifica
 * al cliente que su factura del periodo fue emitida (diagrama DDD 02:
 * Factura -> Notificacion). Idempotente por tabla inbox (lamina 10).
 */
@Component
@Lazy(false)
public class FacturaEmitidaConsumer {

    private static final Logger log = LoggerFactory.getLogger(FacturaEmitidaConsumer.class);
    private static final String CONSUMIDOR = "notificaciones-consumer#facturas-emitidas";

    private final NotificarClienteUseCase useCase;
    private final InboxPort inbox;
    private final ObjectMapper objectMapper;

    public FacturaEmitidaConsumer(NotificarClienteUseCase useCase, InboxPort inbox,
                                  ObjectMapper objectMapper) {
        this.useCase = useCase;
        this.inbox = inbox;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "facturas-emitidas", groupId = "notificaciones-consumer")
    public void escuchar(String mensaje) {
        try {
            String t = mensaje == null ? "" : mensaje.trim();
            if (!t.startsWith("{")) return;
            JsonNode n = objectMapper.readTree(t);
            if (!n.hasNonNull("facturaId")) return;
            String facturaId = n.get("facturaId").asText();
            String clave = n.hasNonNull("eventId") ? n.get("eventId").asText()
                    : "FacturaEmitida|" + facturaId;
            if (!inbox.registrarSiEsNuevo(CONSUMIDOR, clave, "FacturaEmitida")) {
                log.info("FacturaEmitida duplicada ignorada. facturaId={}", facturaId);
                return;
            }
            String serial = n.hasNonNull("medidorSerial") ? n.get("medidorSerial").asText() : null;
            String periodo = n.hasNonNull("periodo") ? n.get("periodo").asText() : null;
            double monto = n.hasNonNull("monto") ? n.get("monto").asDouble() : 0.0;
            useCase.notificarFacturaEmitida(new FacturaEmitida(facturaId, serial, periodo, monto));
            log.info("Notificacion por FacturaEmitida. facturaId={} serial={}", facturaId, serial);
        } catch (Exception e) {
            log.error("Error procesando FacturaEmitida. payload={}", mensaje, e);
        }
    }
}
