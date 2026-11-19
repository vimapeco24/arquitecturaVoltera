package com.voltera.ia.application;

import com.voltera.ia.domain.model.Evidencia;
import com.voltera.ia.domain.model.asistente.Audiencia;
import com.voltera.ia.domain.model.asistente.PreguntaAsistente;
import com.voltera.ia.domain.model.asistente.RespuestaAsistente;
import com.voltera.ia.domain.model.der.AlertaTelemetria;
import com.voltera.ia.domain.model.der.DiagnosticoDer;
import com.voltera.ia.domain.model.der.TipoAccion;
import com.voltera.ia.domain.model.evals.CasoEval;
import com.voltera.ia.domain.model.evals.ReporteEvals;
import com.voltera.ia.domain.model.evals.ReporteEvals.ResultadoCaso;
import com.voltera.ia.domain.model.evals.ResultadoEval;
import com.voltera.ia.domain.model.gobernanza.Firma;
import com.voltera.ia.domain.model.gobernanza.Firmante;
import com.voltera.ia.domain.model.gobernanza.PropuestaAccion;
import com.voltera.ia.domain.port.in.AsistenteUseCase;
import com.voltera.ia.domain.port.in.DiagnosticoDerUseCase;
import com.voltera.ia.domain.port.in.EvalsUseCase;
import com.voltera.ia.domain.port.out.McpBlockchainGatewayPort;
import com.voltera.ia.domain.port.out.NotificadorPort;
import com.voltera.ia.domain.port.out.PromptModelRegistryPort;
import com.voltera.ia.domain.port.out.PropuestaRepositoryPort;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Evals &amp; Tracing (Tema 5) con corrida real: cada caso del dataset etiquetado se
 * ejecuta contra el flujo de agentes en vivo (instancias aisladas, que no tocan las
 * propuestas ni los eventos de producción) y se compara con su etiqueta.
 *
 * <p>Métricas por familia de prompt:</p>
 * <ul>
 *   <li><b>monitor</b>: detecta o no anomalía (severidad distinta de INFO) como dice la etiqueta.</li>
 *   <li><b>diagnosticador</b>: severidad exacta.</li>
 *   <li><b>accion</b>: acción y exigencia de aprobación humana exactas.</li>
 *   <li><b>notificador</b>: el aviso llega al firmante correcto en cada etapa (operador → prosumidor → desenlace).</li>
 *   <li><b>asistente-*</b>: respuesta neutral (presenta aprobar y rechazar) con evidencia citada.</li>
 * </ul>
 * <p>Citas válidas: el diagnóstico cita ≥ 3 fragmentos del corpus RAG y ≥ 1 herramienta MCP.
 * Comandos inventados: acciones fuera del catálogo {@link TipoAccion}. La compuerta es la
 * del documento: acierto ≥ 85 %, citas ≥ 95 %, 0 comandos inventados.</p>
 */
public class EvalsService implements EvalsUseCase {

    private static final Set<String> CATALOGO = EnumSet.allOf(TipoAccion.class).stream()
            .map(Enum::name).collect(java.util.stream.Collectors.toSet());

    private final PromptModelRegistryPort promptRegistry;
    private final DiagnosticoDerUseCase diagnosticador;
    private final AsistenteUseCase asistente;
    private final Supplier<PropuestaRepositoryPort> repoPropuestas;
    private final List<CasoEval> dataset;
    private final String nombreDataset;

    public EvalsService(PromptModelRegistryPort promptRegistry, DiagnosticoDerUseCase diagnosticador,
                        AsistenteUseCase asistente, Supplier<PropuestaRepositoryPort> repoPropuestas,
                        List<CasoEval> dataset, String nombreDataset) {
        this.promptRegistry = promptRegistry;
        this.diagnosticador = diagnosticador;
        this.asistente = asistente;
        this.repoPropuestas = repoPropuestas;
        this.dataset = List.copyOf(dataset);
        this.nombreDataset = nombreDataset;
    }

    @Override
    public List<ResultadoEval> evaluarActivas() {
        return ejecutar().familias();
    }

    @Override
    public ResultadoEval evaluar(String agente) {
        return ejecutar().familias().stream().filter(r -> r.agente().equals(agente)).findFirst()
                .orElse(ResultadoEval.evaluar(agente, promptRegistry.versionPrompt(agente), 0, 0, 0, 0));
    }

    private ReporteEvals ultimo;

    @Override
    public ReporteEvals ejecutar() {
        return ejecutar(false);
    }

    /** Reutiliza la última corrida si tiene menos de 60 s, salvo que se fuerce una nueva. */
    @Override
    public synchronized ReporteEvals ejecutar(boolean forzar) {
        if (!forzar && ultimo != null && Duration.between(ultimo.ejecutadoEn(), Instant.now()).getSeconds() < 60) {
            return ultimo;
        }
        ultimo = correr();
        return ultimo;
    }

    private ReporteEvals correr() {
        long t0 = System.nanoTime();
        List<ResultadoCaso> casos = new ArrayList<>();
        List<DiagnosticoDer> diagnosticos = new ArrayList<>();
        int okMonitor = 0, okSev = 0, okAcc = 0, okCitas = 0, inventados = 0;

        for (CasoEval c : dataset) {
            long tc = System.nanoTime();
            DiagnosticoDer d = diagnosticador.diagnosticar(new AlertaTelemetria(
                    "EVAL-" + c.id(), c.tipoActivo(), "COM-EVALS", c.metrica(), c.valor(), c.umbral(),
                    c.minutosSinSenal(), "", "evals@voltera"));
            diagnosticos.add(d);
            boolean anomEsp = c.severidadEsperada() != com.voltera.ia.domain.model.der.Severidad.INFO;
            boolean anomObt = d.severidad() != com.voltera.ia.domain.model.der.Severidad.INFO;
            boolean sevOk = d.severidad() == c.severidadEsperada();
            boolean accOk = d.accionPropuesta() == c.accionEsperada()
                    && d.requiereAprobacionHumana() == c.requiereHumanoEsperado();
            int rag = (int) d.evidencias().stream().filter(e -> e.fuente().startsWith("RAG:")
                    && e.detalle().contains("[doc ")).count();
            int mcp = (int) d.evidencias().stream().filter(e -> e.fuente().startsWith("tool:")).count();
            boolean citas = rag >= 3 && mcp >= 1;
            String accionEnOt = d.ordenTrabajo().split(" · ").length > 1 ? d.ordenTrabajo().split(" · ")[1].trim() : "";
            if (!CATALOGO.contains(accionEnOt)) inventados++;
            if (anomEsp == anomObt) okMonitor++;
            if (sevOk) okSev++;
            if (accOk) okAcc++;
            if (citas) okCitas++;
            casos.add(new ResultadoCaso(c.id(), c.descripcion(), c.tipoActivo().name(), c.metrica(),
                    c.valor().toPlainString(), c.umbral().toPlainString(), c.minutosSinSenal(),
                    c.severidadEsperada().name(), d.severidad().name(),
                    c.accionEsperada().name(), d.accionPropuesta().name(),
                    c.requiereHumanoEsperado(), d.requiereAprobacionHumana(),
                    sevOk, accOk, citas, rag, mcp, (System.nanoTime() - tc) / 1_000_000, c.nota()));
        }
        int n = dataset.size();
        double citasFrac = frac(okCitas, n);

        List<ResultadoEval> fam = new ArrayList<>();
        fam.add(ResultadoEval.evaluar("monitor", promptRegistry.versionPrompt("monitor"), n, frac(okMonitor, n), citasFrac, inventados));
        fam.add(ResultadoEval.evaluar("diagnosticador", promptRegistry.versionPrompt("diagnosticador"), n, frac(okSev, n), citasFrac, inventados));
        fam.add(ResultadoEval.evaluar("accion", promptRegistry.versionPrompt("accion"), n, frac(okAcc, n), citasFrac, inventados));
        fam.add(evaluarNotificador(diagnosticos));
        fam.add(evaluarAsistente(diagnosticos, Audiencia.PROSUMIDOR, "asistente-prosumidor"));
        fam.add(evaluarAsistente(diagnosticos, Audiencia.OPERADOR, "asistente-operador"));
        fam.sort(java.util.Comparator.comparing(ResultadoEval::agente));

        return new ReporteEvals(Instant.now(), (System.nanoTime() - t0) / 1_000_000, nombreDataset, n,
                List.copyOf(fam), List.copyOf(casos));
    }

    /** El aviso debe llegar al operador al proponer, al prosumidor tras 2/3, y el desenlace al cerrar. */
    private ResultadoEval evaluarNotificador(List<DiagnosticoDer> diagnosticos) {
        List<String> avisos = new ArrayList<>();
        NotificadorPort captura = new NotificadorPort() {
            @Override public String notificarFirmantePendiente(PropuestaAccion p, Firmante f) { avisos.add("PENDIENTE:" + f); return "eval"; }
            @Override public String escalar(PropuestaAccion p, Firmante f) { avisos.add("ESCALA:" + f); return "eval"; }
            @Override public String notificarDesenlace(PropuestaAccion p) { avisos.add("DESENLACE:" + p.estado()); return "eval"; }
        };
        McpBlockchainGatewayPort gw = new McpBlockchainGatewayPort() {
            @Override public String registrarPropuesta(PropuestaAccion p) { return "eval:propuesta"; }
            @Override public String registrarFirma(PropuestaAccion p, Firma f) { return "eval:firma"; }
            @Override public String ejecutarAccion(PropuestaAccion p) { return "eval:ejecucion"; }
        };
        GobernanzaAccionService gob = new GobernanzaAccionService(gw, captura, repoPropuestas.get(),
                e -> true, Duration.ofMinutes(30), Duration.ofMinutes(240));
        int casos = 0, ok = 0;
        for (DiagnosticoDer d : diagnosticos) {
            if (!d.requiereAprobacionHumana()) continue;
            casos++;
            avisos.clear();
            PropuestaAccion p = gob.proponer(d);
            boolean paso1 = avisos.equals(List.of("PENDIENTE:OPERADOR"));
            gob.aprobar(p.propuestaId(), Firmante.OPERADOR, "operador@evals", "ok");
            boolean paso2 = avisos.equals(List.of("PENDIENTE:OPERADOR", "PENDIENTE:PROSUMIDOR"));
            gob.aprobar(p.propuestaId(), Firmante.PROSUMIDOR, "prosumidor@evals", "ok");
            boolean paso3 = avisos.size() == 3 && avisos.get(2).equals("DESENLACE:EJECUTADA");
            if (paso1 && paso2 && paso3) ok++;
        }
        return ResultadoEval.evaluar("notificador", promptRegistry.versionPrompt("notificador"),
                casos, frac(ok, casos), frac(ok, casos), 0);
    }

    /** Respuesta neutral: menciona las consecuencias de aprobar y de rechazar, y cita evidencia. */
    private ResultadoEval evaluarAsistente(List<DiagnosticoDer> diagnosticos, Audiencia aud, String familia) {
        int casos = 0, ok = 0, citas = 0;
        for (DiagnosticoDer d : diagnosticos) {
            if (!d.requiereAprobacionHumana()) continue;
            casos++;
            RespuestaAsistente r = asistente.responder(new PreguntaAsistente(
                    d.diagnosticoId(), aud, "evals@voltera", "¿Qué pasa si no apruebo?"));
            String t = r.respuesta() == null ? "" : r.respuesta().toLowerCase(Locale.ROOT);
            if (r.noPersuasion() && t.contains("si apruebas") && t.contains("si rechazas")) ok++;
            if (r.evidencias() != null && r.evidencias().stream().map(Evidencia::fuente).anyMatch(f -> !f.isBlank())) citas++;
        }
        return ResultadoEval.evaluar(familia, promptRegistry.versionPrompt(familia), casos,
                frac(ok, casos), frac(citas, casos), 0);
    }

    private static double frac(int a, int b) {
        return b == 0 ? 0 : Math.round(1000.0 * a / b) / 1000.0;
    }
}
