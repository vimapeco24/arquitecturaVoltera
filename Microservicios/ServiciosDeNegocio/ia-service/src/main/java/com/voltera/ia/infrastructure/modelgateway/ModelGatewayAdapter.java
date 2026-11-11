package com.voltera.ia.infrastructure.modelgateway;

import com.voltera.ia.domain.model.Evidencia;
import com.voltera.ia.domain.model.SolicitudCopiloto;
import com.voltera.ia.domain.port.out.ModelGatewayPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Model Gateway: unico punto por el que pasa toda llamada a un LLM. Aplica ruteo
 * por proveedor, presupuesto de tokens (max-tokens), timeout, cabecera de
 * zero-retention y respaldo (si falla, cae al motor local via Optional.empty()).
 *
 * <p>Proveedor por defecto = "none": la demo funciona sin credenciales y, lo mas
 * importante, NO envia ningun dato fuera del clúster. Con provider=openai-compatible
 * y api-key, llama a un endpoint /chat/completions estandar.</p>
 */
@Component
public class ModelGatewayAdapter implements ModelGatewayPort {

    private static final Logger log = LoggerFactory.getLogger(ModelGatewayAdapter.class);

    private final String provider;
    private final String model;
    private final String apiKey;
    private final int maxTokens;
    private final Duration timeout;
    private final boolean zeroRetention;
    private final WebClient client;

    public ModelGatewayAdapter(
            @Value("${ia.model-gateway.provider:none}") String provider,
            @Value("${ia.model-gateway.base-url:https://api.openai.com/v1}") String baseUrl,
            @Value("${ia.model-gateway.api-key:}") String apiKey,
            @Value("${ia.model-gateway.model:gpt-4o-mini}") String model,
            @Value("${ia.model-gateway.max-tokens:600}") int maxTokens,
            @Value("${ia.model-gateway.timeout-ms:8000}") long timeoutMs,
            @Value("${ia.model-gateway.zero-retention:true}") boolean zeroRetention,
            WebClient.Builder builder) {
        this.provider = provider == null ? "none" : provider.trim().toLowerCase();
        this.model = model;
        this.apiKey = apiKey;
        this.maxTokens = maxTokens;
        this.timeout = Duration.ofMillis(timeoutMs);
        this.zeroRetention = zeroRetention;
        this.client = builder.clone().baseUrl(baseUrl).build();
    }

    @Override
    public boolean proveedorActivo() {
        return !"none".equals(provider) && apiKey != null && !apiKey.isBlank();
    }

    @Override
    public String identificadorMotor() {
        return proveedorActivo() ? "llm:" + model : "local";
    }

    @Override
    public Optional<String> redactarJustificacion(SolicitudCopiloto solicitud, List<Evidencia> evidencias) {
        if (!proveedorActivo()) {
            return Optional.empty();
        }
        try {
            String prompt = construirPrompt(solicitud, evidencias);
            Map<String, Object> payload = Map.of(
                    "model", model,
                    "max_tokens", maxTokens,
                    "temperature", 0.2,
                    "messages", List.of(
                            Map.of("role", "system", "content",
                                    "Eres el Copiloto de Siniestros de Voltera. Propones, no decides. "
                                            + "Responde en español, en un parrafo, citando la evidencia. "
                                            + "La decision final siempre es de un analista humano."),
                            Map.of("role", "user", "content", prompt)));

            var req = client.post().uri("/chat/completions")
                    .header("Authorization", "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON);
            if (zeroRetention) {
                // Cabecera best-effort; proveedores que la soportan no retienen el prompt.
                req = req.header("X-Zero-Retention", "true");
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> resp = req.bodyValue(payload)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(timeout)
                    .onErrorResume(e -> {
                        log.warn("Model Gateway: fallo la llamada al LLM, se usa respaldo local: {}", e.getMessage());
                        return Mono.empty();
                    })
                    .block();

            return extraerContenido(resp);
        } catch (Exception e) {
            log.warn("Model Gateway: excepcion, se usa respaldo local: {}", e.getMessage());
            return Optional.empty();
        }
    }

    @SuppressWarnings("unchecked")
    private Optional<String> extraerContenido(Map<String, Object> resp) {
        if (resp == null) return Optional.empty();
        Object choices = resp.get("choices");
        if (choices instanceof List<?> list && !list.isEmpty()
                && list.get(0) instanceof Map<?, ?> first
                && first.get("message") instanceof Map<?, ?> msg
                && msg.get("content") instanceof String content
                && !content.isBlank()) {
            return Optional.of(content.trim());
        }
        return Optional.empty();
    }

    private String construirPrompt(SolicitudCopiloto s, List<Evidencia> evidencias) {
        StringBuilder sb = new StringBuilder();
        sb.append("Caso de siniestro energetico P2P.\n");
        sb.append("Siniestro: ").append(s.siniestroId()).append("\n");
        sb.append("Poliza: ").append(s.polizaId()).append("\n");
        sb.append("Monto reclamado (COP): ").append(s.montoReclamacion().toPlainString()).append("\n");
        sb.append("Descripcion (ya enmascarada): ").append(s.descripcion()).append("\n\n");
        sb.append("Evidencia recuperada:\n");
        for (Evidencia e : evidencias) {
            sb.append("- [").append(e.fuente()).append("] ").append(e.titulo())
              .append(": ").append(e.detalle()).append("\n");
        }
        sb.append("\nRedacta una recomendacion (propuesta) para el analista, citando la evidencia.");
        return sb.toString();
    }
}
