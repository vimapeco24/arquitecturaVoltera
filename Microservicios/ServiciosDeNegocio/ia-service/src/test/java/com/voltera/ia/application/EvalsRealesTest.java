package com.voltera.ia.application;

import com.voltera.ia.domain.model.evals.ReporteEvals;
import com.voltera.ia.domain.model.evals.ResultadoEval;
import com.voltera.ia.infrastructure.evals.DatasetEvalsLoader;
import com.voltera.ia.infrastructure.guardrails.RegexGuardrails;
import com.voltera.ia.infrastructure.knowledge.Bm25ConocimientoDer;
import com.voltera.ia.infrastructure.persistence.InMemoryDiagnosticoRepository;
import com.voltera.ia.infrastructure.persistence.InMemoryPropuestaRepository;
import com.voltera.ia.infrastructure.registry.InMemoryAgentRegistry;
import com.voltera.ia.infrastructure.registry.InMemoryPromptModelRegistry;
import com.voltera.ia.domain.model.Evidencia;
import com.voltera.ia.domain.port.out.ModelGatewayPort;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** La corrida de Evals ejecuta el dataset real contra el flujo y detecta los fallos conocidos (SoH). */
class EvalsRealesTest {

    @Test
    void corridaRealDetectaLosCasosQueElMotorFalla() {
        var registry = new InMemoryPromptModelRegistry();
        var repo = new InMemoryDiagnosticoRepository();
        ModelGatewayPort sinLlm = new ModelGatewayPort() {
            @Override public Optional<String> redactarJustificacion(com.voltera.ia.domain.model.SolicitudCopiloto s, List<Evidencia> e) { return Optional.empty(); }
            @Override public boolean proveedorActivo() { return false; }
            @Override public String identificadorMotor() { return "local"; }
        };
        var diag = new DiagnosticoDerService(new Bm25ConocimientoDer(),
                a -> List.of(Evidencia.de("tool:telemetria", "t", "d")), new InMemoryAgentRegistry(),
                new RegexGuardrails(), sinLlm, registry, d -> { }, e -> true, repo);
        var dataset = new DatasetEvalsLoader();
        var evals = new EvalsService(registry, diag, new AsistenteService(repo, new RegexGuardrails(), registry),
                InMemoryPropuestaRepository::new, dataset.casos(), dataset.nombre());

        ReporteEvals r = evals.ejecutar();
        assertEquals(29, r.totalCasos());
        assertEquals(6, r.familias().size());
        ResultadoEval sev = r.familias().stream().filter(f -> f.agente().equals("diagnosticador")).findFirst().orElseThrow();
        assertEquals(27.0 / 29, sev.acierto(), 0.001);
        assertTrue(sev.apta());
        List<String> fallidos = r.casos().stream().filter(c -> !c.severidadOk()).map(ReporteEvals.ResultadoCaso::metrica).toList();
        assertEquals(List.of("SoH", "SoH"), fallidos);
        assertFalse(r.casos().stream().anyMatch(c -> !c.citasOk()));
    }
}
