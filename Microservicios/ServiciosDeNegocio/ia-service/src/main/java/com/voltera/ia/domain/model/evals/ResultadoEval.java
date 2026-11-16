package com.voltera.ia.domain.model.evals;

/**
 * Resultado de evaluar una versión de prompt contra el conjunto de diagnósticos
 * de referencia (Tema 5 · Evals & Tracing). Los umbrales del documento son:
 * ≥ 85 % de acierto, ≥ 95 % de citas válidas y 0 comandos inventados. Una versión
 * solo se puede publicar si {@code apta} es true.
 *
 * @param agente            familia de prompt evaluada (p. ej. "diagnosticador").
 * @param version           versión evaluada (p. ej. "diagnosticador@v3").
 * @param casos             número de casos de referencia.
 * @param acierto           fracción de aciertos [0..1].
 * @param citasValidas      fracción de respuestas con citas válidas [0..1].
 * @param comandosInventados número de comandos inventados (debe ser 0).
 * @param apta              true si pasa los tres umbrales.
 */
public record ResultadoEval(
        String agente,
        String version,
        int casos,
        double acierto,
        double citasValidas,
        int comandosInventados,
        boolean apta
) {
    public static final double UMBRAL_ACIERTO = 0.85;
    public static final double UMBRAL_CITAS = 0.95;

    public static ResultadoEval evaluar(String agente, String version, int casos,
                                        double acierto, double citasValidas, int comandosInventados) {
        boolean apta = acierto >= UMBRAL_ACIERTO
                && citasValidas >= UMBRAL_CITAS
                && comandosInventados == 0;
        return new ResultadoEval(agente, version, casos, acierto, citasValidas, comandosInventados, apta);
    }

    public String veredicto() {
        if (apta) return "APTA para publicar";
        StringBuilder sb = new StringBuilder("NO apta: ");
        if (acierto < UMBRAL_ACIERTO) sb.append("acierto < 85%; ");
        if (citasValidas < UMBRAL_CITAS) sb.append("citas < 95%; ");
        if (comandosInventados > 0) sb.append("hay comandos inventados; ");
        return sb.toString().trim();
    }
}
