package com.voltera.ia.application;

import com.voltera.ia.domain.model.evals.ResultadoEval;
import com.voltera.ia.domain.port.in.EvalsUseCase;
import com.voltera.ia.domain.port.out.PromptModelRegistryPort;

import java.util.List;
import java.util.Map;

/**
 * Evals &amp; Tracing (Tema 5 · énfasis del grupo). Corre un conjunto de
 * diagnósticos de referencia contra la versión activa de cada familia de prompt y
 * aplica los umbrales del documento (acierto ≥ 85 %, citas ≥ 95 %, 0 comandos
 * inventados). Decide si una versión es APTA para publicarse; de lo contrario, el
 * Prompt &amp; Model Registry mantiene la versión anterior (rollback).
 *
 * <p>En esta implementación las métricas provienen de un conjunto de referencia
 * fijo por familia (determinista, sin red): suficiente para demostrar la compuerta
 * de calidad. En producción el conjunto vive en el repositorio de evals y la
 * corrida llama al modelo a través del Model Gateway.</p>
 */
public class EvalsService implements EvalsUseCase {

    private final PromptModelRegistryPort promptRegistry;

    /** Conjunto de referencia por familia: {casos, acierto, citas, comandosInventados}. */
    private static final Map<String, double[]> REFERENCIA = Map.of(
            "diagnosticador", new double[]{40, 0.92, 0.98, 0},
            "accion",         new double[]{30, 0.90, 0.97, 0},
            "monitor",        new double[]{50, 0.88, 1.00, 0},
            "notificador",    new double[]{20, 0.95, 1.00, 0},
            "asistente-prosumidor", new double[]{25, 0.93, 0.96, 0},
            "asistente-operador",   new double[]{25, 0.94, 0.97, 0});

    public EvalsService(PromptModelRegistryPort promptRegistry) {
        this.promptRegistry = promptRegistry;
    }

    @Override
    public List<ResultadoEval> evaluarActivas() {
        return REFERENCIA.keySet().stream().sorted().map(this::evaluar).toList();
    }

    @Override
    public ResultadoEval evaluar(String agente) {
        double[] m = REFERENCIA.get(agente);
        String version = promptRegistry.versionPrompt(agente);
        if (m == null) {
            // Sin conjunto de referencia definido: no apta por falta de evidencia.
            return ResultadoEval.evaluar(agente, version, 0, 0, 0, 0);
        }
        return ResultadoEval.evaluar(agente, version, (int) m[0], m[1], m[2], (int) m[3]);
    }
}
