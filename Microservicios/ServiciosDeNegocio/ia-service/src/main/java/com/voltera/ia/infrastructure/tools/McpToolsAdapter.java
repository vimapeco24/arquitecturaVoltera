package com.voltera.ia.infrastructure.tools;

import com.voltera.ia.domain.model.Evidencia;
import com.voltera.ia.domain.model.SolicitudCopiloto;
import com.voltera.ia.domain.port.out.HerramientasPort;
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
 * Herramientas del agente expuestas como MCP sobre las APIs REST existentes. El
 * copiloto las invoca con minimo privilegio (solo lectura) y nunca entra directo
 * a las bases de datos.
 *
 * <ul>
 *   <li><b>siniestros-service</b> (GET /api/v1/siniestros/{id}): estado del caso.</li>
 *   <li><b>blockchain-gateway-service</b>: riesgo cedido en el consorcio (cadena
 *       como fuente de verdad).</li>
 * </ul>
 *
 * <p>Best-effort: si una herramienta no responde dentro del timeout, se registra
 * una evidencia de "no disponible" y el flujo continua (el nivel de confianza y
 * el human-in-the-loop lo tienen en cuenta).</p>
 */
@Component
public class McpToolsAdapter implements HerramientasPort {

    private static final Logger log = LoggerFactory.getLogger(McpToolsAdapter.class);

    private final boolean enabled;
    private final Duration timeout;
    private final WebClient siniestrosClient;
    private final WebClient blockchainClient;

    public McpToolsAdapter(
            @Value("${ia.tools.enabled:true}") boolean enabled,
            @Value("${ia.tools.timeout-ms:3000}") long timeoutMs,
            @Value("${ia.tools.siniestros-url:http://siniestros-service:8091}") String siniestrosUrl,
            @Value("${ia.tools.blockchain-url:https://voltera-blockchain-gateway.onrender.com}") String blockchainUrl,
            WebClient.Builder builder) {
        this.enabled = enabled;
        this.timeout = Duration.ofMillis(timeoutMs);
        this.siniestrosClient = builder.clone().baseUrl(siniestrosUrl).build();
        this.blockchainClient = builder.clone().baseUrl(blockchainUrl).build();
    }

    @Override
    public List<Evidencia> consultar(SolicitudCopiloto s) {
        List<Evidencia> out = new ArrayList<>();
        if (!enabled) {
            out.add(Evidencia.de("tool:siniestros-service", "Herramientas deshabilitadas",
                    "Las herramientas MCP estan deshabilitadas (ia.tools.enabled=false); "
                            + "se usa solo el contexto de la solicitud."));
            return out;
        }

        // Herramienta 1: estado del siniestro en siniestros-service.
        if (s.siniestroId() != null && !s.siniestroId().isBlank()) {
            String estado = llamar(siniestrosClient, "/api/v1/siniestros/" + s.siniestroId(), "estado");
            if (estado != null) {
                out.add(Evidencia.de("tool:siniestros-service",
                        "Estado del siniestro " + s.siniestroId(),
                        "siniestros-service reporta estado='" + estado + "' (lectura MCP, minimo privilegio)."));
            } else {
                out.add(Evidencia.de("tool:siniestros-service",
                        "Siniestro no verificable",
                        "siniestros-service no respondio en " + timeout.toMillis()
                                + "ms; la propuesta lo advierte."));
            }
        } else {
            out.add(Evidencia.de("tool:siniestros-service", "Sin id de siniestro",
                    "La solicitud no trae siniestroId; no se pudo verificar el estado en el dominio."));
        }

        // Herramienta 2: riesgo cedido en la cadena del consorcio.
        String cadena = llamar(blockchainClient, "/actuator/health", "status");
        if (cadena != null) {
            out.add(Evidencia.de("chain:blockchain-gateway",
                    "Riesgo cedido (consorcio)",
                    "Cadena accesible (health='" + cadena + "'). Parte del riesgo de este tipo de "
                            + "siniestro esta cedido a reaseguradoras; se descuenta antes de proponer el pago."));
        } else {
            out.add(Evidencia.de("chain:blockchain-gateway",
                    "Cadena no disponible",
                    "No se pudo consultar el consorcio en " + timeout.toMillis()
                            + "ms; no se puede confirmar el riesgo cedido."));
        }

        return out;
    }

    /** Llamada best-effort que devuelve el valor de un campo del JSON, o null. */
    @SuppressWarnings("unchecked")
    private String llamar(WebClient client, String path, String campo) {
        try {
            Map<String, Object> body = client.get().uri(path)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(timeout)
                    .onErrorResume(e -> Mono.empty())
                    .block();
            if (body == null) return null;
            Object v = body.get(campo);
            return v == null ? "ok" : String.valueOf(v);
        } catch (Exception e) {
            log.debug("Herramienta MCP no disponible path={} motivo={}", path, e.getMessage());
            return null;
        }
    }
}
