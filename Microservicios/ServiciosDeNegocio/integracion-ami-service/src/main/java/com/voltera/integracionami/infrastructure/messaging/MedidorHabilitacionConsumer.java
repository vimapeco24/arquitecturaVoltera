package com.voltera.integracionami.infrastructure.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.voltera.integracionami.domain.port.in.RecibirLecturasUseCase;
import com.voltera.integracionami.domain.port.out.InboxPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consume el topic {@code medidores-habilitacion} (emitido por habilitacion-service).
 * Reacciona a MedidorHabilitado (empieza a atender el medidor) y MedidorSuspendido
 * (deja de atenderlo). El tipo de evento se infiere del payload (campo 'estado') o,
 * si no está, por heuristica del contenido. Idempotente por tabla inbox (lamina 10).
 */
@Component
@Lazy(false)
public class MedidorHabilitacionConsumer {

    private static final Logger log = LoggerFactory.getLogger(MedidorHabilitacionConsumer.class);
    private static final String CONSUMIDOR = "integracion-ami-consumer#medidores-habilitacion";

    private final RecibirLecturasUseCase useCase;
    private final InboxPort inbox;
    private final ObjectMapper objectMapper;

    public MedidorHabilitacionConsumer(RecibirLecturasUseCase useCase, InboxPort inbox,
                                       ObjectMapper objectMapper) {
        this.useCase = useCase;
        this.inbox = inbox;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "medidores-habilitacion", groupId = "integracion-ami-consumer")
    public void escuchar(String mensaje) {
        try {
            JsonNode n = objectMapper.readTree(normalizar(mensaje));
            String serial = texto(n, "serial", "medidorSerial", "medidorId");
            String estado = texto(n, "estado");
            boolean suspendido = n.has("motivo") && estado == null; // HabilitacionFallida/MedidorSuspendido traen 'motivo'

            if (serial == null) {
                log.debug("Mensaje sin serial/medidorId, ignorado: {}", mensaje);
                return;
            }
            String tipoEvento = suspendido ? "MedidorSuspendido" : "MedidorHabilitado";
            if (!inbox.registrarSiEsNuevo(CONSUMIDOR, ClaveIdempotencia.derivar(n, tipoEvento), tipoEvento)) {
                log.info("Evento de habilitacion duplicado ignorado en ACL. serial={} tipo={}", serial, tipoEvento);
                return;
            }
            if (suspendido) {
                useCase.suspenderMedidor(serial);
                log.info("Medidor suspendido en ACL. serial={}", serial);
            } else {
                useCase.habilitarMedidor(serial);
                log.info("Medidor habilitado en ACL. serial={}", serial);
            }
        } catch (Exception e) {
            log.error("Error procesando evento de habilitacion. payload={}", mensaje, e);
        }
    }

    /** El outbox serializa Map.toString() en algunos casos; intentamos JSON directo. */
    private String normalizar(String mensaje) {
        String t = mensaje == null ? "" : mensaje.trim();
        return t.startsWith("{") ? t : "{}";
    }

    private String texto(JsonNode n, String... campos) {
        for (String c : campos) {
            if (n.hasNonNull(c)) return n.get(c).asText();
        }
        return null;
    }
}
