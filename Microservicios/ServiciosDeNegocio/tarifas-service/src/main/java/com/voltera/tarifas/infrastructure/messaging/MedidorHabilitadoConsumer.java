package com.voltera.tarifas.infrastructure.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.voltera.tarifas.domain.port.in.AsignarTarifaUseCase;
import com.voltera.tarifas.domain.port.out.InboxPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consume {@code medidores-habilitacion}: ante MedidorHabilitado (trae 'estado')
 * asigna una tarifa al medidor y emite TarifaAsignada (diagrama DDD 02). Idempotente
 * por tabla inbox (lamina 10): una reentrega no asigna tarifa dos veces.
 */
@Component
@Lazy(false)
public class MedidorHabilitadoConsumer {

    private static final Logger log = LoggerFactory.getLogger(MedidorHabilitadoConsumer.class);
    private static final String CONSUMIDOR = "tarifas-consumer#medidores-habilitacion";

    private final AsignarTarifaUseCase useCase;
    private final InboxPort inbox;
    private final ObjectMapper objectMapper;

    public MedidorHabilitadoConsumer(AsignarTarifaUseCase useCase, InboxPort inbox,
                                     ObjectMapper objectMapper) {
        this.useCase = useCase;
        this.inbox = inbox;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "medidores-habilitacion", groupId = "tarifas-consumer")
    public void escuchar(String mensaje) {
        try {
            String t = mensaje == null ? "" : mensaje.trim();
            if (!t.startsWith("{")) return;
            JsonNode n = objectMapper.readTree(t);
            if (!n.hasNonNull("estado")) return; // solo MedidorHabilitado
            String serial = n.hasNonNull("serial") ? n.get("serial").asText()
                    : (n.hasNonNull("medidorId") ? n.get("medidorId").asText() : null);
            if (serial == null) return;
            if (!inbox.registrarSiEsNuevo(CONSUMIDOR,
                    ClaveIdempotencia.derivar(n, "MedidorHabilitado"), "MedidorHabilitado")) {
                log.info("MedidorHabilitado duplicado ignorado en tarifas. serial={}", serial);
                return;
            }
            useCase.asignarTarifaAMedidor(serial);
            log.info("Tarifa asignada por MedidorHabilitado. serial={}", serial);
        } catch (Exception e) {
            log.error("Error procesando MedidorHabilitado en tarifas. payload={}", mensaje, e);
        }
    }
}
