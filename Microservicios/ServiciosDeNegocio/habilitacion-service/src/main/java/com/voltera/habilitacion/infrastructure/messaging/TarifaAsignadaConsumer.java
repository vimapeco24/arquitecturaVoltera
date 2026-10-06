package com.voltera.habilitacion.infrastructure.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.voltera.habilitacion.domain.port.in.HabilitarMedidorUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consume {@code tarifas-eventos} y reacciona a {@code TarifaAsignada} (de Tarifas):
 * confirma la tarifa del medidor para avanzar el alta (lamina 02: Habilitacion
 * consume TarifaAsignada).
 */
@Component
@Lazy(false)
public class TarifaAsignadaConsumer {

    private static final Logger log = LoggerFactory.getLogger(TarifaAsignadaConsumer.class);

    private final HabilitarMedidorUseCase useCase;
    private final ObjectMapper objectMapper;

    public TarifaAsignadaConsumer(HabilitarMedidorUseCase useCase, ObjectMapper objectMapper) {
        this.useCase = useCase;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "tarifas-eventos", groupId = "habilitacion-consumer")
    public void escuchar(String mensaje) {
        try {
            String t = mensaje == null ? "" : mensaje.trim();
            if (!t.startsWith("{")) return;
            JsonNode n = objectMapper.readTree(t);
            // Solo TarifaAsignada trae 'tarifaId'; ignoramos otros eventos del topic.
            if (!n.hasNonNull("tarifaId")) return;
            String serialOId = n.hasNonNull("medidorSerial") ? n.get("medidorSerial").asText()
                    : (n.hasNonNull("medidorId") ? n.get("medidorId").asText() : null);
            if (serialOId == null) return;
            useCase.confirmarTarifaPorSerialOId(serialOId);
            log.info("TarifaAsignada confirmada por evento. medidor={}", serialOId);
        } catch (Exception e) {
            log.warn("No se pudo procesar TarifaAsignada. payload={} err={}", mensaje, e.getMessage());
        }
    }
}
