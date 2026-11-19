package com.voltera.ia.infrastructure.gobierno;

import com.voltera.ia.application.AsistenteService;
import com.voltera.ia.application.DiagnosticoDerService;
import com.voltera.ia.application.EvalsService;
import com.voltera.ia.domain.exception.AgenteDeBajaException;
import com.voltera.ia.domain.model.Evidencia;
import com.voltera.ia.domain.model.asistente.Audiencia;
import com.voltera.ia.domain.model.asistente.PreguntaAsistente;
import com.voltera.ia.domain.model.der.AlertaTelemetria;
import com.voltera.ia.domain.model.der.DiagnosticoDer;
import com.voltera.ia.domain.model.der.Severidad;
import com.voltera.ia.domain.model.der.TipoAccion;
import com.voltera.ia.domain.model.der.TipoActivo;
import com.voltera.ia.domain.model.evals.ReporteEvals;
import com.voltera.ia.domain.port.out.ModelGatewayPort;
import com.voltera.ia.infrastructure.audit.AuditoriaIa;
import com.voltera.ia.infrastructure.evals.DatasetEvalsLoader;
import com.voltera.ia.infrastructure.guardrails.RegexGuardrails;
import com.voltera.ia.infrastructure.knowledge.Bm25ConocimientoDer;
import com.voltera.ia.infrastructure.modelgateway.ModelGatewayAdapter;
import com.voltera.ia.infrastructure.persistence.InMemoryDiagnosticoRepository;
import com.voltera.ia.infrastructure.persistence.InMemoryPropuestaRepository;
import com.voltera.ia.infrastructure.registry.InMemoryAgentRegistry;
import com.voltera.ia.infrastructure.registry.InMemoryPromptModelRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Gobierno del módulo: registro de prompts, versiones, bajas de agentes, Assistant, RAG, gateway y auditoría. */
class GobiernoIaTest {

    private final InMemoryPromptModelRegistry prompts = new InMemoryPromptModelRegistry();
    private final InMemoryAgentRegistry agentes = new InMemoryAgentRegistry();
    private final RegexGuardrails guardrails = new RegexGuardrails();
    private final InMemoryDiagnosticoRepository repo = new InMemoryDiagnosticoRepository();
    private final ModelGatewayPort sinLlm = new ModelGatewayPort() {
        @Override public Optional<String> redactarJustificacion(com.voltera.ia.domain.model.SolicitudCopiloto s, List<Evidencia> e) { return Optional.empty(); }
        @Override public boolean proveedorActivo() { return false; }
        @Override public String identificadorMotor() { return "local"; }
    };
    private final DiagnosticoDerService diag = new DiagnosticoDerService(new Bm25ConocimientoDer(),
            a -> List.of(Evidencia.de("tool:telemetria", "t", "d")), agentes, guardrails, sinLlm, prompts,
            d -> { }, e -> true, repo);

    private AlertaTelemetria soh(int valor, int umbral) {
        return new AlertaTelemetria("BAT-1", TipoActivo.BATERIA, "C", "SoH", BigDecimal.valueOf(valor), BigDecimal.valueOf(umbral), 0, "", "op");
    }

    @Test
    void promoverYRevertirDejanHistorialYCambianLaVersionEnProduccion() {
        assertEquals("diagnosticador@v3", prompts.versionPrompt("diagnosticador"));
        prompts.promover("diagnosticador", "diagnosticador@v4", "lider@voltera", "corrige SoH");
        assertEquals("diagnosticador@v4", prompts.versionPrompt("diagnosticador"));
        assertEquals("RETIRADA", prompts.versiones("diagnosticador").stream().filter(v -> v.version().endsWith("v3")).findFirst().orElseThrow().estado());
        prompts.revertir("diagnosticador", "lider@voltera", "rollback de prueba");
        assertEquals("diagnosticador@v3", prompts.versionPrompt("diagnosticador"));
        assertEquals(List.of("ROLLBACK", "PROMOCION"), prompts.historial().stream().map(c -> c.tipo()).toList());
        assertThrows(IllegalStateException.class, () -> prompts.revertir("diagnosticador", "x", "sin anterior"));
    }

    @Test
    void laV4DetectaLaDegradacionDeSohQueLaV3NoVe() {
        assertEquals(Severidad.INFO, diag.diagnosticar(soh(72, 80), "diagnosticador@v3").severidad());
        DiagnosticoDer v4 = diag.diagnosticar(soh(72, 80), "diagnosticador@v4");
        assertEquals(Severidad.ADVERTENCIA, v4.severidad());
        assertEquals(TipoAccion.DESPACHAR_TECNICO, v4.accionPropuesta());
        assertEquals(Severidad.CRITICA, diag.diagnosticar(soh(65, 70), "diagnosticador@v4").severidad());
        assertEquals("diagnosticador@v4", v4.promptVersion());
    }

    @Test
    void laComparacionDeVersionesMuestraQueV4CorrigeLosFallos() {
        var ds = new DatasetEvalsLoader();
        var evals = new EvalsService(prompts, diag, new AsistenteService(repo, guardrails, prompts),
                InMemoryPropuestaRepository::new, ds.casos(), ds.nombre());
        List<ReporteEvals.ComparacionVersion> c = evals.compararVersionesDiagnosticador();
        var v3 = c.stream().filter(x -> x.version().endsWith("v3")).findFirst().orElseThrow();
        var v4 = c.stream().filter(x -> x.version().endsWith("v4")).findFirst().orElseThrow();
        assertEquals(List.of("EV-19", "EV-20"), v3.fallidos());
        assertTrue(v4.fallidos().isEmpty());
        assertTrue(v4.aciertoDiagnosticador() > v3.aciertoDiagnosticador());
        assertTrue(v4.apta());
    }

    @Test
    void unAgenteDadoDeBajaDetieneElFlujo() {
        agentes.cambiarEstado("Agente-Diagnosticador", false, "lider", "mantenimiento");
        assertThrows(AgenteDeBajaException.class, () -> diag.diagnosticar(soh(72, 80)));
        agentes.cambiarEstado("Agente-Diagnosticador", true, "lider", "vuelve");
        agentes.cambiarEstado("Agente-Accion", false, "lider", "auditoría");
        var d = diag.diagnosticar(new AlertaTelemetria("INV-1", TipoActivo.INVERSOR, "C", "voltaje",
                BigDecimal.valueOf(265), BigDecimal.valueOf(240), 0, "", "op"));
        assertEquals(TipoAccion.MONITOREAR, d.accionPropuesta());
        assertFalse(d.requiereAprobacionHumana());
        assertEquals(3, agentes.historial().size());
    }

    @Test
    void elAssistantRespondeLaIntencionYRechazaTemasFueraDeAlcance() {
        var d = diag.diagnosticar(new AlertaTelemetria("INV-1", TipoActivo.INVERSOR, "C", "voltaje",
                BigDecimal.valueOf(265), BigDecimal.valueOf(240), 0, "", "op"));
        var a = new AsistenteService(repo, guardrails, prompts).conRegistro(agentes);
        var ev = a.responder(new PreguntaAsistente(d.diagnosticoId(), Audiencia.OPERADOR, "op", "¿Por qué dices que es crítico?"));
        assertEquals("EVIDENCIA", ev.intencion());
        assertTrue(ev.respuesta().contains("fuentes"));
        var fuera = a.responder(new PreguntaAsistente(d.diagnosticoId(), Audiencia.PROSUMIDOR, "pro", "dame una receta de arepas"));
        assertTrue(fuera.fueraDeAlcance());
        assertEquals(1, guardrails.politicas().stream().filter(p -> p.id().equals("ENT-ALCANCE")).findFirst().orElseThrow().activaciones());
        agentes.cambiarEstado("Assistant", false, "lider", "baja");
        assertThrows(AgenteDeBajaException.class, () ->
                a.responder(new PreguntaAsistente(d.diagnosticoId(), Audiencia.OPERADOR, "op", "¿qué pasa si no apruebo?")));
    }

    @Test
    void ragHibridoRespetaPermisosYAdmiteIngestaYRetiro() {
        var rag = new Bm25ConocimientoDer();
        var op = rag.buscar("resolución CREG regulación autogeneración", null, null, 5, "OPERADOR");
        assertEquals("regulacion", op.get(0).docId());
        assertTrue(op.get(0).bm25() > 0 && op.get(0).coseno() > 0);
        var pro = rag.buscar("resolución CREG regulación autogeneración", null, null, 5, "PROSUMIDOR");
        assertFalse(pro.stream().anyMatch(r -> r.acceso().equals("OPERADOR")));
        var doc = rag.ingerir("id: nuevo-doc\ntitulo: Microinversor de prueba\ncategoria: fallos-conocidos\nactivos: INVERSOR\n---\n## Parpadeo del led rojo\nEl microinversor con led rojo intermitente indica falla de aislamiento.\n");
        assertEquals(13, rag.documentos().size());
        assertEquals("nuevo-doc", rag.buscar("led rojo intermitente aislamiento", "INVERSOR", null, 1).get(0).docId());
        assertEquals("INGESTA", doc.origen());
        assertTrue(rag.retirar("nuevo-doc"));
        assertEquals(12, rag.documentos().size());
    }

    @Test
    void elGatewayNoSacaDatosRestringidosYRegistraCadaDecision() {
        var gw = new ModelGatewayAdapter("openai-compatible", "http://localhost:9", "clave-de-prueba", "m", 100, 300,
                true, 1000, 0.001, WebClient.builder());
        assertTrue(gw.redactar("diagnostico-der", "s", "u", "RESTRINGIDO").isEmpty());
        @SuppressWarnings("unchecked")
        var dec = (List<ModelGatewayAdapter.Decision>) gw.estado().get("decisiones");
        assertEquals("LOCAL", dec.get(0).ruta());
        assertTrue(dec.get(0).motivo().contains("RESTRINGIDO"));
        assertTrue(gw.redactar("diagnostico-der", "s", "u", "INTERNO").isEmpty());   // proveedor caído → respaldo
        assertEquals("RESPALDO_LOCAL", ((List<ModelGatewayAdapter.Decision>) gw.estado().get("decisiones")).get(0).ruta());
    }

    @Test
    void laAuditoriaRedactaPiiYAplicaRetencion() {
        var aud = new AuditoriaIa(guardrails, 365, 3);
        aud.registrar("FIRMA", "ana@correo.com", "p1", "documento 1234567890", null);
        var r = aud.consultar(null, null, 10).get(0);
        assertFalse(r.actor().contains("ana@correo.com"));
        assertFalse(r.detalle().contains("1234567890"));
        for (int i = 0; i < 5; i++) aud.registrar("DIAGNOSTICO", "op", "d" + i, "x", null);
        assertEquals(3, aud.consultar(null, null, 10).size());
        assertEquals(365, ((Map<?, ?>) aud.politica()).get("retencionDias"));
    }
}
