package com.voltera.notificaciones.infrastructure.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.voltera.notificaciones.domain.event.LecturaSospechosaDetectada;
import com.voltera.notificaciones.domain.port.in.NotificarClienteUseCase;
import com.voltera.notificaciones.domain.port.out.InboxPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consume {@code telemetria-alertas} (lámina 03 · CQRS: topic dedicado de alertas
 * donde publican Ingesta y Core). En ese topic conviven LecturaSospechosaDetectada
 * (de Ingesta) y MedidorSinReporte (de Core). Aquí solo reaccionamos a
 * LecturaSospechosaDetectada, que es la única que trae 'motivo' sin 'ultimaLecturaEn'.
 * Idempotente por tabla inbox (lamina 10).
 */
@Component
@Lazy(false)
public class LecturaSospechosaConsumer {

    private static final Logger log = LoggerFactory.getLogger(LecturaSospechosaConsumer.class);
    private static final String CONSUMIDOR = "notificaciones-consumer#telemetria-alertas#sospechosa";

    private final NotificarClienteUseCase useCase;
    private final InboxPort inbox;
    private final ObjectMapper objectMapper;

    public LecturaSospechosaConsumer(NotificarClienteUseCase useCase, InboxPort inbox,
                                     ObjectMapper objectMapper) {
        this.useCase = useCase;
        this.inbox = inbox;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "telemetria-alertas", groupId = "notificaciones-consumer")
    public void escuchar(String mensaje) {
        try {
            String t = mensaje == null ? "" : mensaje.trim();
            if (!t.startsWith("{")) return;
            JsonNode n = objectMapper.readTree(t);
            // Discriminador: la sospechosa trae 'motivo' y NO trae 'ultimaLecturaEn'
            // (ese campo lo trae MedidorSinReporte, la otra alerta del topic).
            boolean esSospechosa = n.hasNonNull("motivo") && !n.hasNonNull("ultimaLecturaEn");
            if (!esSospechosa) return;
            if (!inbox.registrarSiEsNuevo(CONSUMIDOR,
                    ClaveIdempotencia.derivar(n, "LecturaSospechosaDetectada"), "LecturaSospechosaDetectada")) {
                log.info("LecturaSospechosaDetectada duplicada ignorada.");
                return;
            }
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
