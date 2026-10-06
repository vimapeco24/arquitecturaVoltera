package com.voltera.facturacion.infrastructure.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.voltera.facturacion.domain.port.in.FacturarDesdeLiquidacionUseCase;
import com.voltera.facturacion.domain.port.out.InboxPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consume {@code liquidaciones-calculadas}: ante LiquidacionCalculada emite la
 * factura del periodo y publica FacturaEmitida (diagrama DDD 02:
 * Liquidacion -> Factura). Idempotente por tabla inbox (lamina 10): una
 * LiquidacionCalculada repetida no emite dos facturas.
 */
@Component
@Lazy(false)
public class LiquidacionCalculadaConsumer {

    private static final Logger log = LoggerFactory.getLogger(LiquidacionCalculadaConsumer.class);
    private static final String CONSUMIDOR = "facturacion-consumer#liquidaciones-calculadas";

    private final FacturarDesdeLiquidacionUseCase useCase;
    private final InboxPort inbox;
    private final ObjectMapper objectMapper;

    public LiquidacionCalculadaConsumer(FacturarDesdeLiquidacionUseCase useCase, InboxPort inbox,
                                        ObjectMapper objectMapper) {
        this.useCase = useCase;
        this.inbox = inbox;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "liquidaciones-calculadas", groupId = "facturacion-consumer")
    public void escuchar(String mensaje) {
        try {
            String t = mensaje == null ? "" : mensaje.trim();
            if (!t.startsWith("{")) return;
            JsonNode n = objectMapper.readTree(t);
            if (!n.hasNonNull("liquidacionId")) return;
            String liquidacionId = n.get("liquidacionId").asText();
            // La clave natural de idempotencia es el liquidacionId (una factura por liquidacion).
            String clave = n.hasNonNull("eventId") ? n.get("eventId").asText()
                    : "LiquidacionCalculada|" + liquidacionId;
            if (!inbox.registrarSiEsNuevo(CONSUMIDOR, clave, "LiquidacionCalculada")) {
                log.info("LiquidacionCalculada duplicada ignorada. liquidacionId={}", liquidacionId);
                return;
            }
            String serial = n.hasNonNull("medidorSerial") ? n.get("medidorSerial").asText() : null;
            String periodo = n.hasNonNull("periodo") ? n.get("periodo").asText() : null;
            double total = n.hasNonNull("consumoTotalKwh") ? n.get("consumoTotalKwh").asDouble() : 0.0;
            useCase.facturarDesdeLiquidacion(liquidacionId, serial, periodo, total);
            log.info("Factura emitida desde LiquidacionCalculada. liquidacionId={} serial={}", liquidacionId, serial);
        } catch (Exception e) {
            log.error("Error procesando LiquidacionCalculada. payload={}", mensaje, e);
        }
    }
}
