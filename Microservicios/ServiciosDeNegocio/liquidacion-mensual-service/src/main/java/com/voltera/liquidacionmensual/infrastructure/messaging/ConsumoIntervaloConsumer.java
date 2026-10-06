package com.voltera.liquidacionmensual.infrastructure.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.voltera.liquidacionmensual.domain.port.in.AcumularConsumoUseCase;
import com.voltera.liquidacionmensual.domain.port.out.InboxPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consume {@code telemetria-consumo-intervalos}: ante ConsumoIntervaloRegistrado
 * (trae 'consumoNetoKwh') acumula el consumo del medidor para la liquidacion
 * mensual (diagrama DDD 02: SerieDeMedicion -> Liquidacion). Ignora
 * MedidorSinReporte. Idempotente por tabla inbox (lamina 10): un intervalo repetido
 * no se acumula dos veces en la liquidacion.
 */
@Component
@Lazy(false)
public class ConsumoIntervaloConsumer {

    private static final Logger log = LoggerFactory.getLogger(ConsumoIntervaloConsumer.class);
    private static final String CONSUMIDOR = "liquidacion-mensual-consumer#telemetria-consumo-intervalos";

    private final AcumularConsumoUseCase useCase;
    private final InboxPort inbox;
    private final ObjectMapper objectMapper;

    public ConsumoIntervaloConsumer(AcumularConsumoUseCase useCase, InboxPort inbox,
                                    ObjectMapper objectMapper) {
        this.useCase = useCase;
        this.inbox = inbox;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "telemetria-consumo-intervalos", groupId = "liquidacion-mensual-consumer")
    public void escuchar(String mensaje) {
        try {
            String t = mensaje == null ? "" : mensaje.trim();
            if (!t.startsWith("{")) return;
            JsonNode n = objectMapper.readTree(t);
            if (!n.hasNonNull("consumoNetoKwh")) return; // solo ConsumoIntervaloRegistrado
            if (!inbox.registrarSiEsNuevo(CONSUMIDOR,
                    ClaveIdempotencia.derivar(n, "ConsumoIntervaloRegistrado"), "ConsumoIntervaloRegistrado")) {
                log.info("ConsumoIntervaloRegistrado duplicado ignorado en liquidacion.");
                return;
            }
            String serial = n.hasNonNull("medidorSerial") ? n.get("medidorSerial").asText() : null;
            double consumo = n.get("consumoNetoKwh").asDouble();
            useCase.acumular(serial, consumo);
            log.info("Consumo acumulado para liquidacion. serial={} +{} kWh", serial, consumo);
        } catch (Exception e) {
            log.error("Error procesando ConsumoIntervaloRegistrado en liquidacion. payload={}", mensaje, e);
        }
    }
}
