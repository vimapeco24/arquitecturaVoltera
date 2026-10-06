package com.voltera.notificaciones.infrastructure.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.voltera.notificaciones.domain.event.LecturaSospechosaDetectada;
import com.voltera.notificaciones.domain.port.in.NotificarClienteUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consume {@code telemetria-lecturas-validadas}: en ese topic Ingesta publica
 * LecturaValidada, LecturaSospechosaDetectada y CanalIngestaCreado. Aqui solo
 * reaccionamos a LecturaSospechosaDetectada, que es la unica que trae 'motivo'
 * sin 'validadaEn' ni 'creadoEn'.
 */
@Component
@Lazy(false)
public class LecturaSospechosaConsumer {

    private static final Logger log = LoggerFactory.getLogger(LecturaSospechosaConsumer.class);

    private final NotificarClienteUseCase useCase;
    private final ObjectMapper objectMapper;

    public LecturaSospechosaConsumer(NotificarClienteUseCase useCase, ObjectMapper objectMapper) {
        this.useCase = useCase;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "telemetria-lecturas-validadas", groupId = "notificaciones-consumer")
    public void escuchar(String mensaje) {
        try {
            String t = mensaje == null ? "" : mensaje.trim();
            if (!t.startsWith("{")) return;
            JsonNode n = objectMapper.readTree(t);
            // Discriminador: la sospechosa trae 'motivo' y NO trae 'validadaEn' ni 'creadoEn'.
            boolean esSospechosa = n.hasNonNull("motivo")
                    && !n.hasNonNull("validadaEn")
                    && !n.hasNonNull("creadoEn");
            if (!esSospechosa) return;
            String serial = n.hasNonNull("medidorSerial") ? n.get("medidorSerial").asText() : null;
            double consumo = n.hasNonNull("consumoKwh") ? n.get("consumoKwh").asDouble() : 0.0;
            useCase.notificarLecturaSospechosa(
                    new LecturaSospechosaDetectada(serial, consumo, n.get("motivo").asText()));
            log.info("Notificacion por LecturaSospechosaDetectada. serial={}", serial);
        } catch (Exception e) {
            log.error("Error procesando LecturaSospechosaDetectada. payload={}", mensaje, e);
        }
    }
}
