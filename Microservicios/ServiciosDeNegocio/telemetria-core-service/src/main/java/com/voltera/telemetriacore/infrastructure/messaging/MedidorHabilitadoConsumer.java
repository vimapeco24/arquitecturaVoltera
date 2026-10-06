package com.voltera.telemetriacore.infrastructure.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.voltera.telemetriacore.domain.port.in.RegistrarConsumoUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consume {@code medidores-habilitacion}: ante MedidorHabilitado crea la serie del
 * medidor para empezar a acumular su consumo.
 */
@Component
@Lazy(false)
public class MedidorHabilitadoConsumer {

    private static final Logger log = LoggerFactory.getLogger(MedidorHabilitadoConsumer.class);

    private final RegistrarConsumoUseCase useCase;
    private final ObjectMapper objectMapper;

    public MedidorHabilitadoConsumer(RegistrarConsumoUseCase useCase, ObjectMapper objectMapper) {
        this.useCase = useCase;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "medidores-habilitacion", groupId = "telemetria-core-consumer")
    public void escuchar(String mensaje) {
        try {
            String t = mensaje == null ? "" : mensaje.trim();
            if (!t.startsWith("{")) return;
            JsonNode n = objectMapper.readTree(t);
            // Solo MedidorHabilitado trae 'estado'; ignoramos activado/fallida/suspendido aqui.
            if (n.hasNonNull("estado")) {
                String serial = n.hasNonNull("serial") ? n.get("serial").asText()
                        : (n.hasNonNull("medidorId") ? n.get("medidorId").asText() : null);
                if (serial != null) {
                    useCase.crearSerie(serial);
                    log.info("Serie de medicion creada por MedidorHabilitado. serial={}", serial);
                }
            }
        } catch (Exception e) {
            log.error("Error procesando MedidorHabilitado. payload={}", mensaje, e);
        }
    }
}
