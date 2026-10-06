package com.voltera.ingesta.infrastructure.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.voltera.ingesta.domain.port.in.IngestarLecturaUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consume {@code medidores-habilitacion}: ante MedidorHabilitado abre el canal
 * (=> CanalIngestaCreado); ante MedidorSuspendido lo cierra.
 */
@Component
@Lazy(false)
public class MedidorHabilitacionConsumer {

    private static final Logger log = LoggerFactory.getLogger(MedidorHabilitacionConsumer.class);

    private final IngestarLecturaUseCase useCase;
    private final ObjectMapper objectMapper;

    public MedidorHabilitacionConsumer(IngestarLecturaUseCase useCase, ObjectMapper objectMapper) {
        this.useCase = useCase;
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
