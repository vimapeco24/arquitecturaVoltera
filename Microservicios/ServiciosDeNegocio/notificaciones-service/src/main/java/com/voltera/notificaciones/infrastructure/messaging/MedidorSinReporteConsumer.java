package com.voltera.notificaciones.infrastructure.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.voltera.notificaciones.domain.event.MedidorSinReporte;
import com.voltera.notificaciones.domain.port.in.NotificarClienteUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consume {@code telemetria-consumo-intervalos}: en ese topic Telemetria Core
 * publica ConsumoIntervaloRegistrado y MedidorSinReporte. Aqui solo reaccionamos a
 * MedidorSinReporte, que es la unica que trae 'ultimaLecturaEn'.
 */
@Component
@Lazy(false)
public class MedidorSinReporteConsumer {

    private static final Logger log = LoggerFactory.getLogger(MedidorSinReporteConsumer.class);

    private final NotificarClienteUseCase useCase;
    private final ObjectMapper objectMapper;

    public MedidorSinReporteConsumer(NotificarClienteUseCase useCase, ObjectMapper objectMapper) {
        this.useCase = useCase;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "telemetria-consumo-intervalos", groupId = "notificaciones-consumer")
    public void escuchar(String mensaje) {
        try {
            String t = mensaje == null ? "" : mensaje.trim();
            if (!t.startsWith("{")) return;
            JsonNode n = objectMapper.readTree(t);
            // Solo MedidorSinReporte trae 'ultimaLecturaEn'.
            if (!n.hasNonNull("ultimaLecturaEn")) return;
            String serial = n.hasNonNull("medidorSerial") ? n.get("medidorSerial").asText() : null;
            useCase.notificarMedidorSinReporte(
                    new MedidorSinReporte(serial, n.get("ultimaLecturaEn").asText()));
            log.info("Notificacion por MedidorSinReporte. serial={}", serial);
        } catch (Exception e) {
            log.error("Error procesando MedidorSinReporte. payload={}", mensaje, e);
        }
    }
}
