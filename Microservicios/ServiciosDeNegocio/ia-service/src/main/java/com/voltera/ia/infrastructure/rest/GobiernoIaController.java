package com.voltera.ia.infrastructure.rest;

import com.voltera.ia.domain.model.der.AlertaTelemetria;
import com.voltera.ia.domain.model.der.DiagnosticoDer;
import com.voltera.ia.domain.model.der.Severidad;
import com.voltera.ia.domain.model.der.TipoAccion;
import com.voltera.ia.domain.model.evals.CasoEval;
import com.voltera.ia.domain.model.evals.ReporteEvals;
import com.voltera.ia.domain.model.evals.ResultadoEval;
import com.voltera.ia.domain.port.in.EvalsUseCase;
import com.voltera.ia.domain.port.out.AgentRegistryPort;
import com.voltera.ia.domain.port.out.DiagnosticoRepositoryPort;
import com.voltera.ia.domain.port.out.GuardrailsPort;
import com.voltera.ia.domain.port.out.PromptModelRegistryPort;
import com.voltera.ia.infrastructure.audit.AuditoriaIa;
import com.voltera.ia.infrastructure.modelgateway.ModelGatewayAdapter;
import com.voltera.ia.infrastructure.observabilidad.ObservabilidadIa;
import com.voltera.ia.infrastructure.revision.DatasetRetroalimentacion;
import com.voltera.ia.infrastructure.revision.RegistroAlertas;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Gobierno y operación del módulo de IA: Prompt &amp; Model Registry (promoción con
 * compuerta de Evals y rollback), Agent Registry (alta/baja), Human Review →
 * dataset de Evals, Audit &amp; Retention, Guardrails, Model Gateway y observabilidad.
 */
@RestController
@RequestMapping("/api/v1/ia")
public class GobiernoIaController {

    private final PromptModelRegistryPort prompts;
    private final EvalsUseCase evals;
    private final AgentRegistryPort agentes;
    private final DiagnosticoRepositoryPort diagnosticos;
    private final RegistroAlertas alertas;
    private final DatasetRetroalimentacion retro;
    private final AuditoriaIa auditoria;
    private final GuardrailsPort guardrails;
    private final ModelGatewayAdapter gateway;
    private final ObservabilidadIa obs;

    public GobiernoIaController(PromptModelRegistryPort prompts, EvalsUseCase evals, AgentRegistryPort agentes,
                                DiagnosticoRepositoryPort diagnosticos, RegistroAlertas alertas,
                                DatasetRetroalimentacion retro, AuditoriaIa auditoria, GuardrailsPort guardrails,
                                ModelGatewayAdapter gateway, ObservabilidadIa obs) {
        this.prompts = prompts;
        this.evals = evals;
        this.agentes = agentes;
        this.diagnosticos = diagnosticos;
        this.alertas = alertas;
        this.retro = retro;
        this.auditoria = auditoria;
        this.guardrails = guardrails;
        this.gateway = gateway;
        this.obs = obs;
    }

    // ── Prompt & Model Registry ──────────────────────────────────────────────

    public record CambioPromptRequest(@NotBlank String version, @NotBlank String responsable, String motivo) {}

    public record RollbackRequest(@NotBlank String responsable, String motivo) {}

    @GetMapping("/prompts")
    public ResponseEntity<Map<String, Object>> prompts() {
        List<Map<String, Object>> fam = new ArrayList<>();
        for (String f : prompts.familias()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("familia", f);
            m.put("produccion", prompts.versionPrompt(f));
            m.put("versiones", prompts.versiones(f));
            fam.add(m);
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("familias", fam);
        out.put("historial", prompts.historial());
        out.put("compuerta", "acierto ≥ 85 % · citas ≥ 95 % · 0 comandos inventados en monitor, diagnosticador y acción");
        return ResponseEntity.ok(out);
    }

    /** Compara todas las versiones del diagnosticador contra el mismo dataset. */
    @GetMapping("/prompts/diagnosticador/comparacion")
    public ResponseEntity<List<ReporteEvals.ComparacionVersion>> comparacion() {
        return ResponseEntity.ok(evals.compararVersionesDiagnosticador());
    }

    /**
     * Promueve una versión a producción solo si pasa la compuerta de Evals. Para el
     * diagnosticador se corre el dataset con esa versión; si no pasa, se rechaza (409).
     */
    @PostMapping("/prompts/{familia}/promover")
    public ResponseEntity<Map<String, Object>> promover(@PathVariable String familia, @Valid @RequestBody CambioPromptRequest req) {
        Map<String, Object> out = new LinkedHashMap<>();
        if ("diagnosticador".equals(familia)) {
            ReporteEvals r = evals.ejecutarConVersion(req.version());
            List<ResultadoEval> gate = r.familias().stream()
                    .filter(f -> List.of("monitor", "diagnosticador", "accion").contains(f.agente())).toList();
            boolean apta = gate.stream().allMatch(ResultadoEval::apta);
            ReporteEvals actual = evals.ejecutar(true);
            double aciertoActual = actual.familias().stream().filter(f -> f.agente().equals("diagnosticador"))
                    .mapToDouble(ResultadoEval::acierto).findFirst().orElse(0);
            double aciertoCand = gate.stream().filter(f -> f.agente().equals("diagnosticador"))
                    .mapToDouble(ResultadoEval::acierto).findFirst().orElse(0);
            boolean noPeor = aciertoCand >= aciertoActual;
            out.put("compuerta", gate);
            out.put("aciertoProduccion", aciertoActual);
            out.put("aciertoCandidata", aciertoCand);
            if (!apta || !noPeor) {
                out.put("resultado", "RECHAZADA");
                out.put("motivo", (!apta ? "La versión no pasa la compuerta de Evals" : "La versión es peor que la de producción ("
                        + aciertoCand + " < " + aciertoActual + ")") + "; producción se mantiene en " + prompts.versionPrompt(familia));
                auditoria.registrar("PROMOCION_PROMPT", req.responsable(), familia, "rechazada " + req.version() + " (no apta)", req.version());
                return ResponseEntity.status(409).body(out);
            }
        }
        var v = prompts.promover(familia, req.version(), req.responsable(), req.motivo());
        auditoria.registrar("PROMOCION_PROMPT", req.responsable(), familia, "promovida " + v.version()
                + (req.motivo() == null ? "" : " · " + req.motivo()), v.version());
        out.put("resultado", "PROMOVIDA");
        out.put("produccion", v);
        return ResponseEntity.ok(out);
    }

    @PostMapping("/prompts/{familia}/rollback")
    public ResponseEntity<PromptModelRegistryPort.VersionPrompt> rollback(@PathVariable String familia, @Valid @RequestBody RollbackRequest req) {
        var v = prompts.revertir(familia, req.responsable(), req.motivo());
        auditoria.registrar("ROLLBACK_PROMPT", req.responsable(), familia, "vuelve a " + v.version()
                + (req.motivo() == null ? "" : " · " + req.motivo()), v.version());
        return ResponseEntity.ok(v);
    }

    // ── Agent Registry ───────────────────────────────────────────────────────

    public record EstadoAgenteRequest(boolean activo, @NotBlank String responsable, String motivo) {}

    @GetMapping("/agentes/registro")
    public ResponseEntity<Map<String, Object>> registro() {
        return ResponseEntity.ok(Map.of("agentes", agentes.todos(), "historial", agentes.historial()));
    }

    @PostMapping("/agentes/{nombre}/estado")
    public ResponseEntity<AgentRegistryPort.AgenteRegistrado> estadoAgente(@PathVariable String nombre,
                                                                           @Valid @RequestBody EstadoAgenteRequest req) {
        var a = agentes.cambiarEstado(nombre, req.activo(), req.responsable(), req.motivo());
        auditoria.registrar("AGENTE_ESTADO", req.responsable(), a.nombre(),
                (req.activo() ? "alta" : "baja") + (req.motivo() == null ? "" : " · " + req.motivo()), null);
        return ResponseEntity.ok(a);
    }

    // ── Human Review → dataset de Evals ──────────────────────────────────────

    public record RetroalimentacionRequest(@NotBlank String revisor, boolean deAcuerdo, Severidad severidadCorrecta,
                                           TipoAccion accionCorrecta, String comentario) {}

    @PostMapping("/diagnostico/{id}/retroalimentacion")
    public ResponseEntity<Map<String, Object>> retroalimentacion(@PathVariable String id, @Valid @RequestBody RetroalimentacionRequest req) {
        UUID did = UUID.fromString(id);
        DiagnosticoDer d = diagnosticos.porId(did).orElseThrow(() -> new IllegalArgumentException("Diagnóstico no encontrado: " + id));
        AlertaTelemetria a = alertas.porId(did).map(RegistroAlertas.Entrada::alerta)
                .orElseThrow(() -> new IllegalArgumentException("No se conserva la alerta original del diagnóstico " + id));
        Severidad sev = req.deAcuerdo() || req.severidadCorrecta() == null ? d.severidad() : req.severidadCorrecta();
        TipoAccion acc = req.deAcuerdo() || req.accionCorrecta() == null ? d.accionPropuesta() : req.accionCorrecta();
        boolean humano = acc == TipoAccion.COMANDO_FISICO || acc == TipoAccion.CORTAR_SUMINISTRO || acc == TipoAccion.DESPACHAR_TECNICO;
        String casoId = String.format("HR-%02d", retro.siguienteNumero());
        String coment = guardrails.enmascararPII(req.comentario() == null ? "" : req.comentario());
        CasoEval caso = new CasoEval(casoId, "Revisión de " + d.activoId() + " (" + a.metrica() + " " + a.valor().toPlainString()
                + " / umbral " + a.umbral().toPlainString() + ")", a.tipoActivo(), a.metrica(), a.valor(), a.umbral(),
                a.minutosSinSenal(), sev, acc, humano, coment.isBlank() ? "Corrección de " + req.revisor() : coment, CasoEval.HUMAN_REVIEW);
        var r = retro.agregar(new DatasetRetroalimentacion.Revision(UUID.randomUUID().toString(), Instant.now(), id,
                req.revisor(), d.severidad().name(), sev.name(), d.accionPropuesta().name(), acc.name(),
                req.deAcuerdo() && sev == d.severidad() && acc == d.accionPropuesta(), coment, casoId), caso);
        auditoria.registrar("RETROALIMENTACION", req.revisor(), id, (r.deAcuerdo() ? "de acuerdo" : "corrige "
                + d.severidad() + "→" + sev + ", " + d.accionPropuesta() + "→" + acc) + " · caso " + casoId, d.promptVersion());
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("revision", r);
        out.put("casoEval", caso);
        out.put("resumen", retro.resumen());
        return ResponseEntity.ok(out);
    }

    @GetMapping("/retroalimentacion")
    public ResponseEntity<Map<String, Object>> retroalimentaciones() {
        return ResponseEntity.ok(Map.of("resumen", retro.resumen(), "revisiones", retro.revisiones()));
    }

    // ── Audit & Retention · Guardrails · Model Gateway · Observabilidad ──────

    @GetMapping("/auditoria")
    public ResponseEntity<Map<String, Object>> auditoria(@RequestParam(required = false) String tipo,
                                                         @RequestParam(required = false) String recurso,
                                                         @RequestParam(defaultValue = "100") int limite) {
        return ResponseEntity.ok(Map.of("politica", auditoria.politica(), "registros", auditoria.consultar(tipo, recurso, limite)));
    }

    @GetMapping("/guardrails")
    public ResponseEntity<List<GuardrailsPort.Politica>> guardrails() {
        return ResponseEntity.ok(guardrails.politicas());
    }

    @GetMapping("/model-gateway")
    public ResponseEntity<Map<String, Object>> modelGateway() {
        return ResponseEntity.ok(gateway.estado());
    }

    /**
     * KPI del caso de negocio: tiempo medio de detección (desde la lectura observada hasta
     * el diagnóstico), latencia por funcionalidad y duración por paso del flujo.
     */
    @GetMapping("/observabilidad")
    public ResponseEntity<Map<String, Object>> observabilidad() {
        List<RegistroAlertas.Entrada> todas = alertas.todas();
        List<Long> deteccion = todas.stream()
                .map(e -> Math.max(0, Duration.between(e.observadoEn(), e.diagnosticadoEn()).toMillis())).sorted().toList();
        double mediaMs = deteccion.stream().mapToLong(Long::longValue).average().orElse(0);
        Map<String, Object> kpi = new LinkedHashMap<>();
        kpi.put("diagnosticos", deteccion.size());
        kpi.put("mttdSegundos", Math.round(mediaMs / 10.0) / 100.0);
        kpi.put("p95DeteccionSegundos", deteccion.isEmpty() ? 0
                : Math.round(deteccion.get(Math.min(deteccion.size() - 1, (int) Math.ceil(0.95 * deteccion.size()) - 1)) / 10.0) / 100.0);
        kpi.put("lineaBaseMinutos", 45);
        kpi.put("metaMinutos", 5);
        kpi.put("cumpleMeta", mediaMs < 5 * 60_000);
        kpi.put("nota", "La detección se mide desde 'observadoEn' de la alerta (o su llegada si no lo trae) hasta el diagnóstico. Línea base y meta: caso de negocio del taller (45 min → < 5 min).");
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("kpiNegocio", kpi);
        out.put("funcionalidades", obs.funcionalidades());
        out.put("pasosFlujo", obs.pasos());
        Map<String, Object> g = gateway.estado();
        out.put("modelGateway", Map.of("solicitudes", g.get("solicitudes"), "tokensUsados", g.get("tokensUsados"),
                "costoUsd", g.get("costoUsd"), "porRuta", g.get("porRuta")));
        out.put("guardrails", guardrails.politicas());
        return ResponseEntity.ok(out);
    }
}
