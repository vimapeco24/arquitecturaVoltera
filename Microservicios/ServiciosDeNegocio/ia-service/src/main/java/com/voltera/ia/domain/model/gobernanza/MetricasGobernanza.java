package com.voltera.ia.domain.model.gobernanza;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Indicadores agregados de la gobernanza 3/3 para el tablero de la consola:
 * volumen por estado, tasas de aprobación/rechazo y la garantía clave
 * ({@code ejecutadasSin3de3} debe ser siempre 0: fail-closed).
 */
public record MetricasGobernanza(
        int total,
        Map<String, Integer> porEstado,
        int ejecutadas,
        int ejecutadasSin3de3,
        double tasaEjecucion,
        double tasaRechazo,
        double promedioFirmas
) {
    public static MetricasGobernanza desde(List<PropuestaAccion> propuestas) {
        Map<EstadoPropuesta, Integer> c = new EnumMap<>(EstadoPropuesta.class);
        for (EstadoPropuesta e : EstadoPropuesta.values()) c.put(e, 0);
        int sin3 = 0;
        double sumaFirmas = 0;
        for (PropuestaAccion p : propuestas) {
            c.merge(p.estado(), 1, Integer::sum);
            sumaFirmas += p.firmasAprobadas();
            if (p.estado() == EstadoPropuesta.EJECUTADA && p.firmasAprobadas() < 3) sin3++;
        }
        int n = propuestas.size();
        Map<String, Integer> out = new java.util.LinkedHashMap<>();
        c.forEach((k, v) -> out.put(k.name(), v));
        return new MetricasGobernanza(n, out, c.get(EstadoPropuesta.EJECUTADA), sin3,
                n == 0 ? 0 : redondear((double) c.get(EstadoPropuesta.EJECUTADA) / n),
                n == 0 ? 0 : redondear((double) c.get(EstadoPropuesta.RECHAZADA) / n),
                n == 0 ? 0 : redondear(sumaFirmas / n));
    }

    private static double redondear(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
