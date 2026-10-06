package com.voltera.ingesta.infrastructure.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.voltera.ingesta.domain.port.in.IngestarLecturaUseCase;
import com.voltera.ingesta.domain.port.out.InboxPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consume {@code medidores-habilitacion}: ante MedidorHabilitado abre el canal
 * (=> CanalIngestaCreado); ante MedidorSuspendido lo cierra. Idempotente por tabla
 * inbox (lamina 10): una reentrega del mismo evento no reabre/cierra dos veces.
 */
@Component
@Lazy(false)
public class MedidorHabilitacionConsumer {

    private static final Logger log = LoggerFactory.getLogger(MedidorHabilitacionConsumer.class);
    private static final String CONSUMIDOR = "ingesta-consumer#medidores-habilitacion";

    private final IngestarLecturaUseCase useCase;
    private final InboxPort inbox;
    private final ObjectMapper objectMapper;

    public MedidorHabilitacionConsumer(IngestarLecturaUseCase useCase, InboxPort inbox,
                                       ObjectMapper objectMapper) {
        this.useCase = useCase;
        this.inbox = inbox;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "medidores-habilitacion", groupId = "ingesta-consumer")
    public void escuchar(String mensaje) {
        try {
            String t = mensaje == null ? "" : mensaje.trim();
            if (!t.startsWith("{")) return;
            JsonNode n = objectMapper.readTree(t);
            String serial = texto(n, "serial", "medidorSerial", "medidorId");
            String estado = texto(n, "estado");
            boolean suspendido = n.has("motivo") && estado == null;
            if (serial == null) return;

            String tipoEvento = suspendido ? "MedidorSuspendido" : "MedidorHabilitado";
            if (!inbox.registrarSiEsNuevo(CONSUMIDOR, ClaveIdempotencia.derivar(n, tipoEvento), tipoEvento)) {
                log.info("Evento de habilitacion duplicado ignorado. serial={} tipo={}", serial, tipoEvento);
                return;
            }

            if (suspendido) {
                useCase.cerrarCanal(serial);
                log.info("Canal de ingesta cerrado. serial={}", serial);
            } else {
                useCase.abrirCanal(serial);
                log.info("Canal de ingesta abierto. serial={}", serial);
            }
        } catch (Exception e) {
            log.error("Error procesando evento de habilitacion. payload={}", mensaje, e);
        }
    }

    private String texto(JsonNode n, String... campos) {
        for (String c : campos) {
            if (n.hasNonNull(c)) return n.get(c).asText();
        }
        return null;
    }
}
