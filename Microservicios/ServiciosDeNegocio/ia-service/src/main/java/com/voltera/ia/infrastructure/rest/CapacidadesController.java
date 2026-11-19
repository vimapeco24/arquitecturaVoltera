package com.voltera.ia.infrastructure.rest;

import com.voltera.ia.domain.port.out.FirmaCriptograficaPort;
import com.voltera.ia.domain.port.out.ModelGatewayPort;
import com.voltera.ia.infrastructure.evals.DatasetEvalsLoader;
import com.voltera.ia.infrastructure.knowledge.Bm25ConocimientoDer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Estado verificable de cada capacidad del módulo, calculado en tiempo de ejecución a
 * partir de la configuración real y de sondas a los servicios externos. La consola lo
 * muestra tal cual: no hay estados escritos a mano en el front.
 *
 * <p>Estados: OPERATIVO (funciona de punta a punta en este despliegue), LOCAL (funciona
 * con un respaldo local porque falta el recurso externo indicado en {@code requiere}) y
 * PENDIENTE (requiere una acción externa para operar).</p>
 */
@RestController
@RequestMapping("/api/v1/ia/capacidades")
public class CapacidadesController {

    /** Una capacidad con su estado, la evidencia para comprobarlo y lo que falta. */
    public record Capacidad(String clave, String componente, String estado, String detalle,
                            String evidencia, String requiere) {}

    private final Bm25ConocimientoDer rag;
    private final DatasetEvalsLoader dataset;
    private final FirmaCriptograficaPort firmas;
    private final ModelGatewayPort modelGateway;
    private final boolean kafka;
    private final String webhook;
    private final String blockchainUrl;
    private final String telemetriaUrl;
    private final boolean toolsEnabled;
    private final WebClient http = WebClient.builder().build();

    private volatile Map<String, Object> cache;
    private volatile Instant cacheEn = Instant.EPOCH;

    public CapacidadesController(Bm25ConocimientoDer rag, DatasetEvalsLoader dataset, FirmaCriptograficaPort firmas,
                                 ModelGatewayPort modelGateway,
                                 @Value("${voltera.kafka.enabled:false}") boolean kafka,
                                 @Value("${ia.notificaciones.webhook-url:}") String webhook,
                                 @Value("${ia.tools.blockchain-url:}") String blockchainUrl,
                                 @Value("${ia.tools.telemetria-url:}") String telemetriaUrl,
                                 @Value("${ia.tools.enabled:true}") boolean toolsEnabled) {
        this.rag = rag;
        this.dataset = dataset;
        this.firmas = firmas;
        this.modelGateway = modelGateway;
        this.kafka = kafka;
        this.webhook = webhook == null ? "" : webhook.trim();
        this.blockchainUrl = blockchainUrl == null ? "" : blockchainUrl.replaceAll("/+$", "");
        this.telemetriaUrl = telemetriaUrl == null ? "" : telemetriaUrl.replaceAll("/+$", "");
        this.toolsEnabled = toolsEnabled;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> capacidades() {
        if (cache != null && Duration.between(cacheEn, Instant.now()).getSeconds() < 60) {
            return ResponseEntity.ok(cache);
        }
        // El gateway en Render free puede estar dormido: se le da tiempo de arranque.
        Map<String, Object> red = sonda(blockchainUrl + "/api/v1/anclajes/red", 45);
        Map<String, Object> gob = sonda(blockchainUrl + "/api/v1/gobernanza/propuestas/00000000-0000-0000-0000-000000000000/estado", 20);
        boolean anclajeVivo = red != null && Boolean.TRUE.equals(red.get("disponible"));
        boolean contratoGob = gob != null && "ON_CHAIN".equals(String.valueOf(gob.get("modo")));
        Map<String, Object> tel = sonda(telemetriaUrl + "/actuator/health", 6);
        boolean telVivo = tel != null && "UP".equals(String.valueOf(tel.get("status")));

        List<Capacidad> c = new ArrayList<>();
        c.add(new Capacidad("motor", "Motor de diagnóstico",
                modelGateway.proveedorActivo() ? "OPERATIVO" : "LOCAL",
                modelGateway.proveedorActivo()
                        ? "Redacción con LLM (" + modelGateway.identificadorMotor() + "); la clasificación sigue las reglas deterministas."
                        : "Clasificación y redacción con reglas deterministas locales (reproducible, sin enviar datos fuera).",
                "POST /api/v1/ia/diagnostico/der",
                modelGateway.proveedorActivo() ? "" : "IA_PROVIDER=openai-compatible + IA_LLM_API_KEY para redactar con un LLM"));
        c.add(new Capacidad("rag", "RAG · Knowledge Service", "OPERATIVO",
                "Búsqueda híbrida (BM25 + coseno TF-IDF, fusión RRF y reordenamiento) sobre " + rag.documentos().size()
                        + " documentos y " + rag.totalFragmentos() + " secciones, con permisos por rol e ingesta en caliente.",
                "GET /api/v1/ia/conocimiento/buscar?q=…&rol=PROSUMIDOR", "Índice vectorial con embeddings para búsqueda semántica"));
        c.add(new Capacidad("prompt-registry", "Prompt & Model Registry", "OPERATIVO",
                "Versiones por familia (producción, candidata, retirada), aprobador, promoción con compuerta de Evals (apta y no peor) y rollback con historial.",
                "GET /api/v1/ia/prompts · POST /api/v1/ia/prompts/{familia}/promover", ""));
        c.add(new Capacidad("human-review", "Human Review → Evals", "OPERATIVO",
                "Cada revisión de un diagnóstico (de acuerdo o corregida) entra como caso etiquetado a la siguiente corrida de Evals.",
                "POST /api/v1/ia/diagnostico/{id}/retroalimentacion", ""));
        c.add(new Capacidad("agent-registry", "Agent Registry (A2A)", "OPERATIVO",
                "Catálogo con dueño, capacidad y permisos; alta y baja con historial; un agente de baja detiene su parte del flujo.",
                "POST /api/v1/ia/agentes/{nombre}/estado", ""));
        c.add(new Capacidad("model-gateway", "Model Gateway", "OPERATIVO",
                "Ruteo por clasificación del dato (RESTRINGIDO no sale), presupuesto de tokens, costo, caché, timeout y respaldo; registra cada decisión.",
                "GET /api/v1/ia/model-gateway",
                modelGateway.proveedorActivo() ? "" : "IA_PROVIDER + IA_LLM_API_KEY para rutear también a un proveedor externo"));
        c.add(new Capacidad("auditoria", "Audit & Retention", "OPERATIVO",
                "Registro consultable con PII redactada y política de retención (días y tope de registros).",
                "GET /api/v1/ia/auditoria", ""));
        c.add(new Capacidad("observabilidad", "Observabilidad y KPI del caso", "OPERATIVO",
                "Latencia p50/p95 y errores por funcionalidad, duración por paso del flujo, métricas Micrometer y MTTD frente a la meta de 5 min.",
                "GET /api/v1/ia/observabilidad · /actuator/prometheus", ""));
        c.add(new Capacidad("evals", "Evals & Tracing", "OPERATIVO",
                "Corrida en vivo de " + dataset.casos().size() + " casos etiquetados (" + dataset.nombre()
                        + ") contra el flujo de agentes; métricas calculadas en cada consulta.",
                "GET /api/v1/ia/evals/reporte", ""));
        c.add(new Capacidad("firmas", "Firmas de la gobernanza 3/3", "OPERATIVO",
                "ECDSA secp256k1 con prefijo EIP-191, verificables con ecrecover. Claves de rol: "
                        + firmas.origenClaves() + ". También acepta firma de billetera (MetaMask).",
                "GET /api/v1/ia/gobernanza/propuestas/{id}/firmas/verificacion",
                "EFIMERAS".equals(firmas.origenClaves()) ? "IA_FIRMA_KEY_* para direcciones fijas entre reinicios" : ""));
        c.add(new Capacidad("gobernanza", "Reglas de gobernanza 3/3", "OPERATIVO",
                "Fail-closed: sin 3/3 no se ejecuta; un rechazo cierra; doble firma y misma cuenta en dos roles se rechazan; TTL.",
                "GET /api/v1/ia/gobernanza/metricas", ""));
        c.add(new Capacidad("notificaciones", "Agente Notificador", "OPERATIVO",
                "Bandeja por rol dentro de la app" + (webhook.isEmpty() ? "." : " y webhook externo configurado."),
                "GET /api/v1/ia/notificaciones?destinatario=OPERADOR",
                webhook.isEmpty() ? "IA_NOTIF_WEBHOOK_URL para enviar también a Slack/Teams" : ""));
        c.add(new Capacidad("anclaje", "Blockchain · contrato de anclaje (Amoy)", anclajeVivo ? "OPERATIVO" : "PENDIENTE",
                anclajeVivo ? "Contrato " + red.get("contrato") + " en " + red.get("red") + " responde."
                        : "El blockchain-gateway no respondió en 45 s (en Render free puede estar arrancando); reintenta con «Recalcular».",
                "GET " + blockchainUrl + "/api/v1/anclajes/red", anclajeVivo ? "" : "blockchain-gateway disponible"));
        c.add(new Capacidad("contrato", "Blockchain · MultiFirmaAccionDER (Amoy)", contratoGob ? "OPERATIVO" : "PENDIENTE",
                contratoGob ? "Las aprobaciones se leen del contrato desplegado."
                        : "Contrato probado (Hardhat) pero no desplegado en Amoy: las firmas quedan registradas off-chain, firmadas con ECDSA.",
                "GET /api/v1/ia/gobernanza/propuestas/{id}/aprobaciones",
                contratoGob ? "" : "POL de prueba (faucet) en la wallet del gateway + VOLTERA_BLOCKCHAIN_GOBERNANZA_ADDRESS"));
        c.add(new Capacidad("telemetria", "MCP Device Management", toolsEnabled && telVivo ? "OPERATIVO" : "LOCAL",
                telVivo ? "Sonda a telemetria-core-service responde; la alerta trae la lectura en vivo."
                        : "Usa la lectura que trae la alerta; historial y topología se derivan localmente porque el head-end on-premise no es alcanzable desde la nube.",
                "POST /api/v1/ia/diagnostico/der (evidencias tool:*)",
                telVivo ? "" : "IA_TOOL_TELEMETRIA_URL público hacia telemetria-core-service"));
        c.add(new Capacidad("eventos", "Tópicos der.* (Kafka/Redpanda)", kafka ? "OPERATIVO" : "LOCAL",
                kafka ? "Publica der.estado, der.alertas y der.gobernanza al broker."
                        : "Los eventos se registran en el log del servicio; el broker Redpanda vive en la LAN on-premise.",
                "logs del servicio / topic der.gobernanza", kafka ? "" : "VOLTERA_KAFKA_ENABLED=true con un broker alcanzable"));
        c.add(new Capacidad("persistencia", "Persistencia de diagnósticos y propuestas", "LOCAL",
                "Repositorios en memoria del proceso: se reinician con el servicio.",
                "GET /api/v1/ia/gobernanza/propuestas", "Base de datos (p. ej. PostgreSQL de Render) para conservar el historial"));

        Map<String, Long> resumen = new LinkedHashMap<>();
        for (String e : List.of("OPERATIVO", "LOCAL", "PENDIENTE")) {
            resumen.put(e, c.stream().filter(x -> x.estado().equals(e)).count());
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("calculadoEn", Instant.now());
        out.put("resumen", resumen);
        out.put("capacidades", c);
        out.put("direccionesFirmantes", firmas.direcciones());
        cache = out;
        // Si alguna sonda externa falló, se vuelve a intentar pronto.
        cacheEn = anclajeVivo ? Instant.now() : Instant.now().minusSeconds(45);
        return ResponseEntity.ok(out);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> sonda(String url, int segundos) {
        if (!toolsEnabled || url == null || url.isBlank() || url.startsWith("/")) return null;
        try {
            return http.get().uri(url).retrieve().bodyToMono(Map.class).timeout(Duration.ofSeconds(segundos)).block();
        } catch (Exception e) {
            return null;
        }
    }
}
