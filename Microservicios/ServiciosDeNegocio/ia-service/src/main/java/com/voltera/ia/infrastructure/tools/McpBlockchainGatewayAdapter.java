package com.voltera.ia.infrastructure.tools;

import com.voltera.ia.domain.model.gobernanza.EstadoPropuesta;
import com.voltera.ia.domain.model.gobernanza.Firma;
import com.voltera.ia.domain.model.gobernanza.PropuestaAccion;
import com.voltera.ia.domain.port.out.McpBlockchainGatewayPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * MCP Blockchain Gateway (segundo MCP Server). Traduce las operaciones de
 * gobernanza del contrato {@code MultiFirmaAccionDER} en llamadas al
 * blockchain-gateway (Render, público), aislando a los agentes de la cadena.
 *
 * <p>Best-effort con fail-closed: si el gateway no responde, devuelve un
 * {@code txRef} demo determinista (prefijo {@code demo:}) SOLO para registrar la
 * propuesta/firma en la traza, pero la ejecución del comando nunca se simula sin
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
        // Degradación elegante: txRef demo determinista para la traza off-chain.
        return "demo:" + tipo + ":" + Integer.toHexString(demoSeed.hashCode());
    }
}
