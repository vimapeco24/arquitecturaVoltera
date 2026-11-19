package com.voltera.ia.infrastructure.tools;

import com.voltera.ia.domain.model.gobernanza.EstadoPropuesta;
import com.voltera.ia.domain.model.gobernanza.Firma;
import com.voltera.ia.domain.model.gobernanza.LecturaOnChain;
import com.voltera.ia.domain.model.gobernanza.PropuestaAccion;
import com.voltera.ia.domain.port.out.McpBlockchainGatewayPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * MCP Blockchain Gateway (segundo MCP Server). Traduce las operaciones de
 * gobernanza del contrato {@code MultiFirmaAccionDER} en llamadas al
 * blockchain-gateway (Render, público), aislando a los agentes de la cadena.
 *
 * <p>Best-effort con fail-closed: si el gateway no responde, devuelve un
 * {@code txRef} off-chain determinista (prefijo {@code offchain:}) para registrar la
 * propuesta/firma en la traza (la firma ECDSA del rol queda aparte), y la ejecución nunca ocurre sin
 * que la propuesta esté 3/3 (la invariante vive en el agregado y aquí se
 * re-verifica). La red no alcanzable = no se ejecuta (igual que el resto del
 * caso cuando degrada en la nube).</p>
 */
@Component
public class McpBlockchainGatewayAdapter implements McpBlockchainGatewayPort {

    private static final Logger log = LoggerFactory.getLogger(McpBlockchainGatewayAdapter.class);

    private final boolean enabled;
    private final Duration timeout;
    private final WebClient gatewayClient;

    public McpBlockchainGatewayAdapter(
            @Value("${ia.tools.enabled:true}") boolean enabled,
            @Value("${ia.tools.timeout-ms:3000}") long timeoutMs,
            @Value("${ia.tools.blockchain-url:https://voltera-blockchain-gateway.onrender.com}") String url,
            WebClient.Builder builder) {
        this.enabled = enabled;
        this.timeout = Duration.ofMillis(timeoutMs);
        this.gatewayClient = builder.clone().baseUrl(url).build();
    }

    @Override
    public String registrarPropuesta(PropuestaAccion p) {
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("propuestaId", p.propuestaId().toString());
        cuerpo.put("activoId", p.activoId());
        cuerpo.put("tipoAccion", p.tipoAccion().name());
        cuerpo.put("comando", p.comando());
        cuerpo.put("hashDiagnostico", p.hashDiagnostico());
        cuerpo.put("versionPrompt", p.versionPrompt());
        cuerpo.put("versionModelo", p.versionModelo());
        return enviar("/api/v1/gobernanza/propuestas", cuerpo, "propuesta", p.propuestaId().toString());
    }

    @Override
    public String registrarFirma(PropuestaAccion p, Firma firma) {
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("propuestaId", p.propuestaId().toString());
        cuerpo.put("firmante", firma.firmante().name());
        cuerpo.put("aprueba", firma.aprueba());
        cuerpo.put("razon", firma.razon());
        return enviar("/api/v1/gobernanza/firmas", cuerpo, "firma",
                p.propuestaId() + ":" + firma.firmante());
    }

    @Override
    public String ejecutarAccion(PropuestaAccion p) {
        // Fail-closed: se re-verifica la invariante 3/3 antes de ejecutar.
        if (p.estado() != EstadoPropuesta.APROBADA) {
            throw new IllegalStateException(
                    "MCP Blockchain Gateway: no se ejecuta sin 3/3 (estado " + p.estado() + ").");
        }
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("propuestaId", p.propuestaId().toString());
        cuerpo.put("activoId", p.activoId());
        cuerpo.put("comando", p.comando());
        return enviar("/api/v1/gobernanza/ejecuciones", cuerpo, "ejecucion", p.propuestaId().toString());
    }

    /**
     * Lectura solo-lectura del contrato vía GET al blockchain-gateway. Nunca falla:
     * si el gateway está apagado, tarda o responde modo OFF_CHAIN, devuelve OFF_CHAIN.
     */
    @Override
    @SuppressWarnings("unchecked")
    public LecturaOnChain leerEstado(PropuestaAccion p) {
        if (!enabled) return LecturaOnChain.offChain("herramientas deshabilitadas (ia.tools.enabled=false)");
        try {
            Map<String, Object> resp = gatewayClient.get()
                    .uri("/api/v1/gobernanza/propuestas/{id}/estado", p.propuestaId().toString())
                    .retrieve().bodyToMono(Map.class)
                    .timeout(timeout).onErrorResume(e -> Mono.empty()).block();
            if (resp == null) return LecturaOnChain.offChain("blockchain-gateway no respondió");
            return parsearLectura(resp);
        } catch (Exception e) {
            log.debug("Lectura on-chain no disponible: {}", e.getMessage());
            return LecturaOnChain.offChain("lectura on-chain no disponible");
        }
    }

    /** Convierte la respuesta JSON del gateway en {@link LecturaOnChain} (visible para pruebas). */
    @SuppressWarnings("unchecked")
    static LecturaOnChain parsearLectura(Map<String, Object> m) {
        String modo = String.valueOf(m.getOrDefault("modo", LecturaOnChain.OFF_CHAIN));
        List<LecturaOnChain.FirmaOnChain> firmas = new ArrayList<>();
        Object raw = m.get("firmas");
        if (raw instanceof List<?> lista) {
            for (Object o : lista) {
                if (o instanceof Map<?, ?> f) {
                    Object ts = f.get("firmadoEn");
                    Instant cuando = null;
                    try { if (ts != null) cuando = Instant.parse(String.valueOf(ts)); } catch (Exception ignored) { }
                    firmas.add(new LecturaOnChain.FirmaOnChain(
                            str(f.get("rol")), str(f.get("firmante")), str(f.get("txHash")), cuando));
                }
            }
        }
        int aprob = m.get("aprobaciones") instanceof Number n ? n.intValue() : 0;
        return new LecturaOnChain(
                LecturaOnChain.ON_CHAIN.equals(modo) ? LecturaOnChain.ON_CHAIN : LecturaOnChain.OFF_CHAIN,
                Boolean.TRUE.equals(m.get("existe")), str(m.get("estado")), aprob, firmas,
                str(m.get("contrato")), str(m.get("red")), str(m.get("mensaje")));
    }

    private static String str(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    @SuppressWarnings("unchecked")
    private String enviar(String path, Map<String, Object> cuerpo, String tipo, String demoSeed) {
        if (enabled) {
            try {
                Map<String, Object> resp = gatewayClient.post().uri(path)
                        .bodyValue(cuerpo)
                        .retrieve().bodyToMono(Map.class)
                        .timeout(timeout).onErrorResume(e -> Mono.empty()).block();
                if (resp != null) {
                    Object tx = resp.getOrDefault("txHash", resp.get("txRef"));
                    if (tx != null) return String.valueOf(tx);
                    return "chain:" + tipo + ":" + demoSeed;
                }
            } catch (Exception e) {
                log.debug("MCP Blockchain Gateway no disponible path={} motivo={}", path, e.getMessage());
            }
        }
        // Sin contrato alcanzable: referencia off-chain determinista para la traza.
        return "offchain:" + tipo + ":" + Integer.toHexString(demoSeed.hashCode());
    }
}
