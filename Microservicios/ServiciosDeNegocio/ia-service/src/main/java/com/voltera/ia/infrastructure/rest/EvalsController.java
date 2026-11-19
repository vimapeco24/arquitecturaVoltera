package com.voltera.ia.infrastructure.rest;

import com.voltera.ia.domain.model.evals.ReporteEvals;
import com.voltera.ia.domain.model.evals.ResultadoEval;
import com.voltera.ia.domain.port.in.EvalsUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * API de Evals &amp; Tracing (Tema 5). Expone la compuerta de calidad de los
 * prompts: cada versión se evalúa contra el conjunto de referencia y se marca
 * APTA / NO apta según los umbrales (acierto ≥ 85 %, citas ≥ 95 %, 0 comandos
 * inventados). La consola muestra estos resultados por versión.
 */
@RestController
@RequestMapping("/api/v1/ia/evals")
public class EvalsController {

    private final EvalsUseCase evals;

    public EvalsController(EvalsUseCase evals) {
        this.evals = evals;
    }

    @GetMapping
    public ResponseEntity<List<ResultadoEval>> evaluarActivas() {
        return ResponseEntity.ok(evals.evaluarActivas());
    }

    /** Corrida real del dataset: métricas por familia y resultado caso a caso. */
    @GetMapping("/reporte")
    public ResponseEntity<ReporteEvals> reporte(
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "false") boolean forzar) {
        return ResponseEntity.ok(evals.ejecutar(forzar));
    }

    @GetMapping("/{agente}")
    public ResponseEntity<ResultadoEval> evaluar(@PathVariable String agente) {
        return ResponseEntity.ok(evals.evaluar(agente));
    }
}
