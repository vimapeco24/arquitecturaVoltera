package com.voltera.ia.domain.model.evals;

import java.time.Instant;
import java.util.List;

/**
 * Resultado de una corrida real de Evals: cada caso del dataset se ejecuta contra
 * el flujo de agentes en vivo y se compara con su etiqueta.
 *
 * @param ejecutadoEn instante de la corrida.
 * @param duracionMs  tiempo total de la corrida.
 * @param dataset     nombre y versión del dataset.
 * @param totalCasos  casos ejecutados.
 * @param familias    métricas y veredicto de la compuerta por familia de prompt.
 * @param casos       resultado caso a caso (esperado vs obtenido).
 */
public record ReporteEvals(
        Instant ejecutadoEn,
        long duracionMs,
        String dataset,
        int totalCasos,
        List<ResultadoEval> familias,
        List<ResultadoCaso> casos,
        /** Versión del diagnosticador con la que se corrió (la de producción salvo en comparaciones). */
        String versionDiagnosticador,
        /** Cuántos casos vienen de correcciones humanas (Human Review). */
        int casosRetroalimentacion
) {
    /** Resultado de una versión del diagnosticador sobre el mismo dataset (compuerta de promoción). */
    public record ComparacionVersion(String version, String estado, double aciertoMonitor, double aciertoDiagnosticador,
                                     double aciertoAccion, double citas, int inventados, boolean apta,
                                     List<String> fallidos) {}

    /** Resultado de un caso: lo esperado, lo obtenido y si coincide. */
    public record ResultadoCaso(
            String id, String descripcion, String tipoActivo, String metrica,
            String valor, String umbral, int minutosSinSenal,
            String severidadEsperada, String severidadObtenida,
            String accionEsperada, String accionObtenida,
            boolean humanoEsperado, boolean humanoObtenido,
            boolean severidadOk, boolean accionOk, boolean citasOk,
            int fuentesRag, int fuentesMcp, long ms, String nota, String origen
    ) {}
}
