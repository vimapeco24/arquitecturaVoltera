package com.voltera.ia.infrastructure.eventos;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.voltera.ia.domain.model.der.AlertaTelemetria;
import com.voltera.ia.domain.model.der.DiagnosticoDer;
import com.voltera.ia.domain.model.der.TipoActivo;
import com.voltera.ia.domain.port.in.DiagnosticoDerUseCase;
import com.voltera.ia.infrastructure.audit.AuditoriaIa;
import com.voltera.ia.infrastructure.knowledge.Bm25ConocimientoDer;
import com.voltera.ia.infrastructure.observabilidad.ObservabilidadIa;
import com.voltera.ia.infrastructure.revision.RegistroAlertas;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Entrada por EVENTOS de la capa GenAI (ACL). Dos eventos:
 * <ul>
 *   <li>{@code MedidorSinReporte} (topic {@value #TOPIC_ALERTAS}, emitido por telemetria-core-service):
 *       dispara el flujo de diagnóstico. Es idempotente por medidor + instante de detección.</li>
 *   <li>{@code DocumentoConocimientoPublicado} / {@code DocumentoConocimientoRetirado}
 *       (topic {@value #TOPIC_CONOCIMIENTO}): mantienen fresco el índice del RAG (ingesta y supresión propagada).</li>
 * </ul>
 * Lo usan el consumidor Kafka (on-premise) y el puente HTTP {@code POST /api/v1/ia/eventos/...} (nube,
 * donde el broker no es alcanzable). El resultado del diagnóstico se publica por el mismo camino de siempre.
 */
@Service
public class EventosEntrantesService {

    public static final String TOPIC_ALERTAS = "topic.notificaciones";
    public static final String TOPIC_CONOCIMIENTO = "topic.ia.conocimiento";
    private static final String ACTOR = "evento:telemetria-core";

    private static final Logger log = LoggerFactory.getLogger(EventosEntrantesService.class);

    private final DiagnosticoDerUseCase diagnostico;
    private final RegistroAlertas alertas;
    private final ObservabilidadIa obs;
    private final Bm25ConocimientoDer rag;
    private final AuditoriaIa auditoria;
    private final ObjectMapper mapper = new ObjectMapper();
    private final Set<String> vistos = ConcurrentHashMap.newKeySet();

    public EventosEntrantesService(DiagnosticoDerUseCase diagnostico, RegistroAlertas alertas,
                                   ObservabilidadIa obs, Bm25ConocimientoDer rag, AuditoriaIa auditoria) {
        this.diagnostico = diagnostico;
        this.alertas = alertas;
        this.obs = obs;
        this.rag = rag;
        this.auditoria = auditoria;
    }

    /** Procesa un MedidorSinReporte. Devuelve el diagnóstico, o vacío si el evento se ignora o es duplicado. */
    public Optional<DiagnosticoDer> procesarAlerta(String json) {
        JsonNode n = leer(json);
        if (n == null || !n.hasNonNull("medidorSerial")) return Optional.empty();
        String serial = n.get("medidorSerial").asText();
        Instant detectado = instante(n, "detectadoEn", Instant.now());
        Instant ultima = instante(n, "ultimaLecturaEn", detectado);
        if (!vistos.add(serial + "@" + detectado)) {
            log.info("MedidorSinReporte duplicado ignorado medidor={}", serial);
            return Optional.empty();
        }
        int minutos = (int) Math.max(15, Duration.between(ultima, detectado).toMinutes());
        var alerta = new AlertaTelemetria(serial, TipoActivo.MEDIDOR, n.path("comunidadId").asText("sin-comunidad"),
                "ultimaLectura", BigDecimal.valueOf(minutos), BigDecimal.valueOf(15), minutos,
                "Evento MedidorSinReporte desde telemetria-core-service", ACTOR);
        Instant llegada = Instant.now();
        DiagnosticoDer d = diagnostico.diagnosticar(alerta);
        // Tiempo de detección: desde que el broker detectó el silencio, no desde la última lectura.
        Instant observado = detectado.isBefore(llegada) ? detectado : llegada;
        alertas.guardar(d.diagnosticoId(), alerta, observado, d.generadoEn());
        obs.registrarTraza(d.traza(), llegada);
        log.info("Diagnóstico disparado por evento medidor={} diagnostico={}", serial, d.diagnosticoId());
        return Optional.of(d);
    }

    /** Procesa un evento de conocimiento. Devuelve un resumen, o vacío si no es reconocible. */
    public Optional<Map<String, Object>> procesarConocimiento(String json) {
        JsonNode n = leer(json);
        if (n == null) return Optional.empty();
        String tipo = n.path("tipo").asText("");
        String responsable = n.path("responsable").asText("evento");
        if ("DocumentoConocimientoPublicado".equals(tipo) && n.hasNonNull("contenido")) {
            var d = rag.ingerir(n.get("contenido").asText());
            auditoria.registrar("CONOCIMIENTO", responsable, d.id(), "ingesta por evento: " + d.titulo(), null);
            return Optional.of(Map.of("evento", tipo, "documentoId", d.id(), "secciones", d.secciones()));
        }
        if ("DocumentoConocimientoRetirado".equals(tipo) && n.hasNonNull("documentoId")) {
            String id = n.get("documentoId").asText();
            boolean ok = rag.retirar(id);
            if (ok) auditoria.registrar("CONOCIMIENTO", responsable, id, "retirado por evento", null);
            return Optional.of(Map.of("evento", tipo, "documentoId", id, "retirado", ok));
        }
        return Optional.empty();
    }

    private JsonNode leer(String json) {
        try {
            String t = json == null ? "" : json.trim();
            return t.startsWith("{") ? mapper.readTree(t) : null;
        } catch (Exception e) {
            log.warn("Evento no interpretable: {}", e.getMessage());
            return null;
        }
    }

    private static Instant instante(JsonNode n, String campo, Instant defecto) {
        try {
            return n.hasNonNull(campo) ? Instant.parse(n.get(campo).asText()) : defecto;
        } catch (Exception e) {
            return defecto;
        }
    }
}
