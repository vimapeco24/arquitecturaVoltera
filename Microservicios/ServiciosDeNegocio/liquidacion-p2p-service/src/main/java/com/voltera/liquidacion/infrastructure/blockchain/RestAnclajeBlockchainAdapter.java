package com.voltera.liquidacion.infrastructure.blockchain;

import com.voltera.liquidacion.domain.model.TransaccionP2P;
import com.voltera.liquidacion.domain.port.out.AnclajeBlockchainPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Adaptador de SALIDA: llama por REST al blockchain-gateway (que puede vivir en
 * una PaaS externa) para anclar la transaccion P2P. Best-effort y asincrono: no
 * bloquea la liquidacion (gate 6, cadena fuera del camino critico). Si el
 * gateway no responde, se registra y sigue; el gateway tiene su propio OUTBOX,
 * y ademas la via por evento (si Kafka esta disponible) sirve de respaldo.
 */
public class RestAnclajeBlockchainAdapter implements AnclajeBlockchainPort {

    private static final Logger log = LoggerFactory.getLogger(RestAnclajeBlockchainAdapter.class);

    private final RestClient client;
    private final String anclarPath;

    /**
     * @param baseUrl    URL publica del gateway (p. ej. https://voltera-blockchain-gateway.fly.dev)
     *                   o la ruta via WSO2 (https://host:8243/blockchain/v1).
     * @param anclarPath ruta del endpoint de anclaje (por defecto /api/v1/anclajes).
     */
    public RestAnclajeBlockchainAdapter(String baseUrl, String anclarPath) {
        this.client = RestClient.builder().baseUrl(baseUrl).build();
        this.anclarPath = anclarPath;
    }

    @Override
    public void solicitarAnclaje(TransaccionP2P t) {
        // kWh -> Wh (el gateway trabaja en Wh como metadato publico)
        long energiaWh = t.energia().kwh()
                .multiply(BigDecimal.valueOf(1000))
                .setScale(0, RoundingMode.HALF_UP)
                .longValueExact();

        Map<String, Object> body = Map.of(
                "transaccionId", t.id().valor(),
                "vendedorId", t.vendedorId(),
                "compradorId", t.compradorId(),
                "energiaWh", energiaWh,
                "precioPorKwh", t.precioCasacion().porKwh().toPlainString(),
                "valorTotal", t.valorTotal().toPlainString(),
                "liquidadaEn", t.liquidadaEn().toString());

        // Fire-and-forget: no bloquea la respuesta de la liquidacion.
        CompletableFuture.runAsync(() -> {
            try {
                client.post()
                        .uri(anclarPath)
                        .body(body)
                        .retrieve()
                        .toBodilessEntity();
                log.info("Anclaje solicitado al gateway transaccion={}", t.id().valor());
            } catch (Exception e) {
                log.warn("No se pudo solicitar anclaje al gateway transaccion={}: {}",
                        t.id().valor(), e.getMessage());
            }
        });
    }
}
