package com.voltera.blockchain.infrastructure.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.voltera.blockchain.domain.model.LiquidacionP2P;
import com.voltera.blockchain.domain.port.in.AnclarLiquidacionUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Adaptador de ENTRADA (EDA): escucha el evento de liquidacion P2P emitido por
 * el servicio liquidacion-p2p y dispara el anclaje en la cadena. Es el disparo
 * automatico del flujo (el REST queda como camino manual/PoC).
 *
 * <p>Solo se activa si voltera.kafka.enabled=true. Consume de forma tolerante:
 * lee el payload como JSON generico para no acoplarse al DTO exacto del emisor.</p>
 */
@Component
@ConditionalOnProperty(name = "voltera.kafka.enabled", havingValue = "true")
public class LiquidacionCalculadaListener {

    private static final Logger log = LoggerFactory.getLogger(LiquidacionCalculadaListener.class);

    private final AnclarLiquidacionUseCase anclar;
    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

    public LiquidacionCalculadaListener(AnclarLiquidacionUseCase anclar) {
        this.anclar = anclar;
    }

    @KafkaListener(
            topics = "${voltera.blockchain.topic-entrada:topic.facturacion.liquidacion}",
            groupId = "blockchain-gateway")
    public void onLiquidacion(String payload) {
        try {
            JsonNode n = mapper.readTree(payload);

            String txId = texto(n, "transaccionId", "liquidacionId", "id");
            if (txId == null) {
                log.warn("Evento de liquidacion sin id reconocible; se ignora. payload={}", payload);
                return;
            }

            LiquidacionP2P liq = new LiquidacionP2P(
                    txId,
                    textoOr(n, "vendedorId", "prosumidorVendedor", "N/A"),
                    textoOr(n, "compradorId", "prosumidorComprador", "N/A"),
                    largo(n, "energiaWh", "energiaKwh", 1L),
                    decimal(n, "precioPorKwh", "precio"),
                    decimal(n, "valorTotal", "total"),
                    instante(n, "liquidadaEn", "calculadaEn"));

            anclar.anclar(liq);
            log.info("Liquidacion recibida por evento y encolada para anclaje transaccion={}", txId);
        } catch (Exception e) {
            log.error("No se pudo procesar el evento de liquidacion: {}", e.getMessage());
        }
    }

    private static String texto(JsonNode n, String... campos) {
        for (String c : campos) if (n.hasNonNull(c)) return n.get(c).asText();
        return null;
    }

    private static String textoOr(JsonNode n, String a, String b, String def) {
        String v = texto(n, a, b);
        return v == null ? def : v;
    }

    private static long largo(JsonNode n, String wh, String kwh, long def) {
        if (n.hasNonNull(wh)) return n.get(wh).asLong();
        if (n.hasNonNull(kwh)) return Math.round(n.get(kwh).asDouble() * 1000.0); // kWh -> Wh
        return def;
    }

    private static BigDecimal decimal(JsonNode n, String... campos) {
        String v = texto(n, campos);
        return v == null ? BigDecimal.ZERO : new BigDecimal(v);
    }

    private static Instant instante(JsonNode n, String... campos) {
        String v = texto(n, campos);
        try {
            return v == null ? Instant.now() : Instant.parse(v);
        } catch (Exception e) {
            return Instant.now();
        }
    }
}
