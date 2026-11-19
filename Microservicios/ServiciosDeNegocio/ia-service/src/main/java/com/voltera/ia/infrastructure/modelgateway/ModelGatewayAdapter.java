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
    private final long presupuestoTokens;
    private final double precioPor1k;

    /** Decisión de ruteo de una solicitud al gateway. */
    public record Decision(java.time.Instant instante, String funcionalidad, String clasificacion, String ruta,
                           String modelo, String motivo, int tokensEntrada, int tokensSalida, double costoUsd,
                           boolean cache, long latenciaMs) {}

    private final java.util.Deque<Decision> decisiones = new java.util.concurrent.ConcurrentLinkedDeque<>();
    private final java.util.Map<String, String> cache = java.util.Collections.synchronizedMap(
            new java.util.LinkedHashMap<>(64, 0.75f, true) {
                @Override protected boolean removeEldestEntry(java.util.Map.Entry<String, String> e) { return size() > 200; }
            });
    private final java.util.concurrent.atomic.AtomicLong tokensUsados = new java.util.concurrent.atomic.AtomicLong();
    private final java.util.concurrent.atomic.AtomicLong solicitudes = new java.util.concurrent.atomic.AtomicLong();
    private final java.util.concurrent.atomic.AtomicLong aciertosCache = new java.util.concurrent.atomic.AtomicLong();
    private volatile double costoAcumulado = 0;

    public ModelGatewayAdapter(
            @Value("${ia.model-gateway.provider:none}") String provider,
            @Value("${ia.model-gateway.base-url:https://api.openai.com/v1}") String baseUrl,
            @Value("${ia.model-gateway.api-key:}") String apiKey,
            @Value("${ia.model-gateway.model:gpt-4o-mini}") String model,
            @Value("${ia.model-gateway.max-tokens:600}") int maxTokens,
            @Value("${ia.model-gateway.timeout-ms:8000}") long timeoutMs,
            @Value("${ia.model-gateway.zero-retention:true}") boolean zeroRetention,
            @Value("${ia.model-gateway.presupuesto-tokens:200000}") long presupuestoTokens,
            @Value("${ia.model-gateway.precio-usd-1k-tokens:0.0006}") double precioPor1k,
            WebClient.Builder builder) {
        this.presupuestoTokens = presupuestoTokens;
        this.precioPor1k = precioPor1k;
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

    private static int tokens(String s) {
        return s == null ? 0 : Math.max(1, s.length() / 4);   // estimación estándar ~4 caracteres por token
    }

    private void anotar(Decision d) {
        decisiones.addFirst(d);
        while (decisiones.size() > 100) decisiones.pollLast();
    }

    /**
     * Ruteo del gateway: RESTRINGIDO nunca sale a un proveedor externo; sin proveedor o
     * sin presupuesto se usa el motor local; si la misma solicitud ya se respondió se usa la caché.
     */
    @Override
    public Optional<String> redactar(String funcionalidad, String promptSistema, String promptUsuario, String clasificacion) {
        long t0 = System.nanoTime();
        solicitudes.incrementAndGet();
        int tin = tokens(promptSistema) + tokens(promptUsuario);
        String motivo;
        if ("RESTRINGIDO".equalsIgnoreCase(clasificacion)) motivo = "dato RESTRINGIDO (PII): no sale del servicio";
        else if (!proveedorActivo()) motivo = "sin proveedor externo configurado (IA_PROVIDER=none)";
        else if (tokensUsados.get() + tin > presupuestoTokens) motivo = "presupuesto de tokens agotado";
        else motivo = null;
        if (motivo != null) {
            anotar(new Decision(java.time.Instant.now(), funcionalidad, clasificacion, "LOCAL", "reglas-locales",
                    motivo, 0, 0, 0, false, (System.nanoTime() - t0) / 1_000_000));
            return Optional.empty();
        }
        String clave = Integer.toHexString((promptSistema + "|" + promptUsuario).hashCode());
        String enCache = cache.get(clave);
        if (enCache != null) {
            aciertosCache.incrementAndGet();
            anotar(new Decision(java.time.Instant.now(), funcionalidad, clasificacion, "CACHE", model,
                    "misma solicitud respondida antes", 0, 0, 0, true, (System.nanoTime() - t0) / 1_000_000));
            return Optional.of(enCache);
        }
        Optional<String> r = llamar(promptSistema, promptUsuario);
        int tout = r.map(ModelGatewayAdapter::tokens).orElse(0);
        double costo = (tin + tout) / 1000.0 * precioPor1k;
        if (r.isPresent()) {
            tokensUsados.addAndGet(tin + tout);
            synchronized (this) { costoAcumulado += costo; }
            cache.put(clave, r.get());
        }
        anotar(new Decision(java.time.Instant.now(), funcionalidad, clasificacion, r.isPresent() ? "PROVEEDOR" : "RESPALDO_LOCAL",
                model, r.isPresent() ? "proveedor externo con zero-retention=" + zeroRetention : "el proveedor falló o expiró el timeout",
                tin, tout, r.isPresent() ? costo : 0, false, (System.nanoTime() - t0) / 1_000_000));
        return r;
    }

    /** Estado del gateway para la consola: configuración, consumo y últimas decisiones. */
    public java.util.Map<String, Object> estado() {
        java.util.Map<String, Object> m = new java.util.LinkedHashMap<>();
        m.put("proveedor", provider);
        m.put("modelo", model);
        m.put("proveedorActivo", proveedorActivo());
        m.put("zeroRetention", zeroRetention);
        m.put("maxTokens", maxTokens);
        m.put("timeoutMs", timeout.toMillis());
        m.put("presupuestoTokens", presupuestoTokens);
        m.put("tokensUsados", tokensUsados.get());
        m.put("costoUsd", Math.round(costoAcumulado * 10000.0) / 10000.0);
        m.put("precioUsd1kTokens", precioPor1k);
        m.put("solicitudes", solicitudes.get());
        m.put("aciertosCache", aciertosCache.get());
        java.util.Map<String, Long> porRuta = new java.util.LinkedHashMap<>();
        for (Decision d : decisiones) porRuta.merge(d.ruta(), 1L, Long::sum);
        m.put("porRuta", porRuta);
        m.put("reglas", List.of(
                "RESTRINGIDO (la entrada traía PII) → nunca sale: motor local",
                "INTERNO → proveedor externo si está configurado y hay presupuesto",
                "misma solicitud → caché (no se paga dos veces)",
                "timeout o error del proveedor → respaldo local"));
        m.put("decisiones", List.copyOf(decisiones));
        return m;
    }

    private Optional<String> llamar(String sistema, String usuario) {
        try {
            Map<String, Object> payload = Map.of("model", model, "max_tokens", maxTokens, "temperature", 0.2,
                    "messages", List.of(Map.of("role", "system", "content", sistema), Map.of("role", "user", "content", usuario)));
            var req = client.post().uri("/chat/completions").header("Authorization", "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON);
            if (zeroRetention) req = req.header("X-Zero-Retention", "true");
            @SuppressWarnings("unchecked")
            Map<String, Object> resp = req.bodyValue(payload).retrieve().bodyToMono(Map.class).timeout(timeout)
                    .onErrorResume(e -> { log.warn("Model Gateway: fallo del proveedor, respaldo local: {}", e.getMessage()); return Mono.empty(); })
                    .block();
            return extraerContenido(resp);
        } catch (Exception e) {
            log.warn("Model Gateway: excepción, respaldo local: {}", e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public Optional<String> redactarJustificacion(SolicitudCopiloto solicitud, List<Evidencia> evidencias) {
        if (!proveedorActivo()) {
            anotar(new Decision(java.time.Instant.now(), "copiloto-siniestros", "INTERNO", "LOCAL", "reglas-locales",
                    "sin proveedor externo configurado (IA_PROVIDER=none)", 0, 0, 0, false, 0));
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
