package com.voltera.notificaciones.infrastructure.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.voltera.notificaciones.domain.event.MedidorSinReporte;
import com.voltera.notificaciones.domain.port.in.NotificarClienteUseCase;
import com.voltera.notificaciones.domain.port.out.InboxPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consume {@code telemetria-alertas} (lámina 03 · CQRS: topic dedicado de alertas
 * donde publican Ingesta y Core). En ese topic conviven MedidorSinReporte (de Core)
 * y LecturaSospechosaDetectada (de Ingesta). Aquí solo reaccionamos a
 * MedidorSinReporte, que es la única que trae 'ultimaLecturaEn'. Idempotente por
 * tabla inbox (lamina 10).
 */
@Component
@Lazy(false)
public class MedidorSinReporteConsumer {

    private static final Logger log = LoggerFactory.getLogger(MedidorSinReporteConsumer.class);
    private static final String CONSUMIDOR = "notificaciones-consumer#telemetria-alertas#sinreporte";

    private final NotificarClienteUseCase useCase;
    private final InboxPort inbox;
    private final ObjectMapper objectMapper;

    public MedidorSinReporteConsumer(NotificarClienteUseCase useCase, InboxPort inbox,
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
            // Solo MedidorSinReporte trae 'ultimaLecturaEn'.
            if (!n.hasNonNull("ultimaLecturaEn")) return;
            if (!inbox.registrarSiEsNuevo(CONSUMIDOR,
                    ClaveIdempotencia.derivar(n, "MedidorSinReporte"), "MedidorSinReporte")) {
                log.info("MedidorSinReporte duplicado ignorado.");
                return;
            }
            String serial = n.hasNonNull("medidorSerial") ? n.get("medidorSerial").asText() : null;
            useCase.notificarMedidorSinReporte(
                    new MedidorSinReporte(serial, n.get("ultimaLecturaEn").asText()));
            log.info("Notificacion por MedidorSinReporte. serial={}", serial);
        } catch (Exception e) {
            log.error("Error procesando MedidorSinReporte. payload={}", mensaje, e);
        }
    }
}
