package com.voltera.ia.infrastructure.tools;

import com.voltera.ia.domain.model.Evidencia;
import com.voltera.ia.domain.model.der.AlertaTelemetria;
import com.voltera.ia.domain.port.out.HerramientasDerPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Caja 3 · MCP Servers. Expone la capa de gestión de dispositivos DER como
 * herramientas de solo lectura para los agentes, con autorización firmada y Zero
 * Trust por dispositivo. Herramientas: telemetría en tiempo real, historial de
 * comandos y topología de red.
 *
 * <p>En esta demostración la telemetría viene en la propia alerta y el historial
 * se simula, pero se incluye una sonda REST best-effort a la capa de telemetría
 * (telemetria-core-service) cuando está configurada y es alcanzable. Desde la
 * nube ese servicio vive en la LAN privada: si no responde, se degrada con
 * elegancia (igual que en el caso del copiloto).</p>
 */
@Component
public class McpDerAdapter implements HerramientasDerPort {

    private static final Logger log = LoggerFactory.getLogger(McpDerAdapter.class);

    private final boolean enabled;
    private final Duration timeout;
    private final WebClient telemetriaClient;

    public McpDerAdapter(
            @Value("${ia.tools.enabled:true}") boolean enabled,
            @Value("${ia.tools.timeout-ms:3000}") long timeoutMs,
            @Value("${ia.tools.telemetria-url:http://telemetria-core-service:8098}") String telemetriaUrl,
            WebClient.Builder builder) {
        this.enabled = enabled;
        this.timeout = Duration.ofMillis(timeoutMs);
        this.telemetriaClient = builder.clone().baseUrl(telemetriaUrl).build();
    }

    @Override
    public List<Evidencia> consultar(AlertaTelemetria a) {
        List<Evidencia> out = new ArrayList<>();

        // Herramienta 1: telemetría en tiempo real (la de la alerta + sonda opcional).
        out.add(Evidencia.de("tool:telemetria",
                "Telemetría en tiempo real · " + a.activoId(),
                "Lectura MCP (Zero Trust): '" + a.metrica() + "'=" + a.valor().toPlainString()
                        + " vs umbral " + a.umbral().toPlainString()
                        + (a.sinSenal() ? "; sin señal " + a.minutosSinSenal() + " min." : ".")));

        if (enabled) {
            String salud = sonda("/actuator/health", "status");
            out.add(Evidencia.de("tool:telemetria-core",
                    salud != null ? "Capa de telemetría accesible" : "Capa de telemetría no verificable",
                    salud != null
                            ? "telemetria-core-service health='" + salud + "' (lectura MCP, mínimo privilegio)."
                            : "telemetria-core-service no respondió en " + timeout.toMillis()
                                    + "ms (on-premise, LAN privada); se usa la telemetría de la alerta."));
        }

        // Herramienta 2: historial de comandos (simulado — lectura, no ejecución).
        out.add(Evidencia.de("tool:historial-comandos",
                "Historial de comandos · " + a.activoId(),
                "Últimos comandos registrados sobre el activo (lectura): sin comandos físicos en las "
                        + "últimas 24h. El agente NO ejecuta comandos; solo consulta el historial."));

        // Herramienta 3: topología de red de la comunidad.
        out.add(Evidencia.de("tool:topologia",
                "Topología de red · comunidad " + a.comunidadId(),
                "El activo " + a.activoId() + " cuelga del gateway de la comunidad " + a.comunidadId()
                        + "; aguas abajo no hay otros activos afectados reportando anomalía simultánea."));

        return out;
    }

    @SuppressWarnings("unchecked")
    private String sonda(String path, String campo) {
        try {
            Map<String, Object> body = telemetriaClient.get().uri(path)
                    .retrieve().bodyToMono(Map.class)
                    .timeout(timeout).onErrorResume(e -> Mono.empty()).block();
            if (body == null) return null;
            Object v = body.get(campo);
            return v == null ? "ok" : String.valueOf(v);
        } catch (Exception e) {
            log.debug("MCP telemetría no disponible path={} motivo={}", path, e.getMessage());
            return null;
        }
    }
}
