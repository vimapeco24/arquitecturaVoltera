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

import java.time.Instant;

/**
 * Consume {@code telemetria-consumo-intervalos}: ante ConsumoIntervaloRegistrado
 * (trae 'consumoNetoKwh' e 'inicioIntervalo') tarifica el consumo del intervalo
 * (diagrama DDD 02: Tarifas consume ConsumoIntervaloRegistrado). Ignora
 * MedidorSinReporte (que trae 'ultimaLecturaEn'). Idempotente por tabla inbox
 * (lamina 10): un intervalo repetido no se tarifica dos veces.
 */
@Component
@Lazy(false)
public class ConsumoIntervaloConsumer {

    private static final Logger log = LoggerFactory.getLogger(ConsumoIntervaloConsumer.class);
    private static final String CONSUMIDOR = "tarifas-consumer#telemetria-consumo-intervalos";

    private final AsignarTarifaUseCase useCase;
    private final InboxPort inbox;
    private final ObjectMapper objectMapper;

    public ConsumoIntervaloConsumer(AsignarTarifaUseCase useCase, InboxPort inbox,
                                    ObjectMapper objectMapper) {
        this.useCase = useCase;
        this.inbox = inbox;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "telemetria-consumo-intervalos", groupId = "tarifas-consumer")
    public void escuchar(String mensaje) {
        try {
            String t = mensaje == null ? "" : mensaje.trim();
            if (!t.startsWith("{")) return;
            JsonNode n = objectMapper.readTree(t);
            if (!n.hasNonNull("consumoNetoKwh")) return; // solo ConsumoIntervaloRegistrado
            if (!inbox.registrarSiEsNuevo(CONSUMIDOR,
                    ClaveIdempotencia.derivar(n, "ConsumoIntervaloRegistrado"), "ConsumoIntervaloRegistrado")) {
                log.info("ConsumoIntervaloRegistrado duplicado ignorado en tarifas.");
                return;
            }
            String serial = n.hasNonNull("medidorSerial") ? n.get("medidorSerial").asText() : null;
            double consumo = n.get("consumoNetoKwh").asDouble();
            int hora = horaDe(n);
            useCase.registrarConsumoTarificado(serial, consumo, hora);
            log.info("Consumo tarificado. serial={} consumoNetoKwh={}", serial, consumo);
        } catch (Exception e) {
            log.error("Error procesando ConsumoIntervaloRegistrado en tarifas. payload={}", mensaje, e);
        }
    }

    private int horaDe(JsonNode n) {
        try {
            if (n.hasNonNull("inicioIntervalo")) {
                return Instant.parse(n.get("inicioIntervalo").asText())
                        .atZone(java.time.ZoneOffset.UTC).getHour();
            }
        } catch (Exception ignored) { /* usa 0 por defecto */ }
        return 0;
    }
}
