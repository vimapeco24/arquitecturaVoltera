package com.voltera.ia.domain.port.in;

import com.voltera.ia.domain.model.evals.ReporteEvals;
import com.voltera.ia.domain.model.evals.ResultadoEval;

import java.util.List;

/**
 * Evals &amp; Tracing (Tema 5). Ejecuta las pruebas automáticas de cada versión de
 * prompt contra el conjunto de diagnósticos de referencia antes de publicarla y
 * decide si es apta (acierto ≥ 85 %, citas ≥ 95 %, 0 comandos inventados).
 */
public interface EvalsUseCase {

    /** Evalúa la versión activa de cada familia de prompt del caso. */
    List<ResultadoEval> evaluarActivas();

    /** Evalúa una familia de prompt concreta (p. ej. "diagnosticador"). */
    ResultadoEval evaluar(String agente);

    /** Corre el dataset completo contra el flujo en vivo y devuelve el detalle caso a caso. */
    ReporteEvals ejecutar();

    /** Igual que {@link #ejecutar()}, forzando una corrida nueva si {@code forzar} es true. */
    ReporteEvals ejecutar(boolean forzar);
}
