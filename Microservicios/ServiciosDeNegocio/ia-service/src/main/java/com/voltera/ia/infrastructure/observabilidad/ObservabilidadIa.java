package com.voltera.ia.infrastructure.observabilidad;

import com.voltera.ia.domain.model.PasoFlujo;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Evals &amp; Tracing (operación): latencia y errores por funcionalidad del módulo de IA y
 * duración de cada paso del flujo agéntico. Publica también en Micrometer
 * ({@code ia_funcionalidad_seconds}) para Prometheus/Grafana.
 */
@Component
public class ObservabilidadIa {

    private static final int VENTANA = 500;

    private static final class Serie {
        final Deque<Long> lat = new ArrayDeque<>();
        long llamadas, errores;
    }

    private final Map<String, Serie> series = new ConcurrentHashMap<>();
    private final Map<String, Serie> pasos = new ConcurrentHashMap<>();
    private final MeterRegistry meters;

    public ObservabilidadIa(MeterRegistry meters) {
        this.meters = meters;
    }

    public void registrar(String funcionalidad, long ms, boolean error) {
        Serie s = series.computeIfAbsent(funcionalidad, k -> new Serie());
        synchronized (s) {
            s.llamadas++;
            if (error) s.errores++;
            s.lat.addLast(ms);
            if (s.lat.size() > VENTANA) s.lat.pollFirst();
        }
        Timer.builder("ia_funcionalidad").tag("funcionalidad", funcionalidad).tag("error", String.valueOf(error))
                .register(meters).record(Duration.ofMillis(ms));
    }

    /** Duración de cada paso: diferencia entre marcas de tiempo consecutivas de la traza. */
    public void registrarTraza(List<PasoFlujo> traza, java.time.Instant inicio) {
        java.time.Instant prev = inicio;
        for (PasoFlujo p : traza) {
            long ms = Math.max(0, Duration.between(prev, p.ejecutadoEn()).toMillis());
            Serie s = pasos.computeIfAbsent(p.nodo(), k -> new Serie());
            synchronized (s) {
                s.llamadas++;
                s.lat.addLast(ms);
                if (s.lat.size() > VENTANA) s.lat.pollFirst();
            }
            prev = p.ejecutadoEn();
        }
    }

    static long percentil(List<Long> ordenados, double p) {
        if (ordenados.isEmpty()) return 0;
        int i = (int) Math.ceil(p * ordenados.size()) - 1;
        return ordenados.get(Math.max(0, Math.min(i, ordenados.size() - 1)));
    }

    private static Map<String, Object> resumen(String nombre, Serie s) {
        List<Long> l;
        long llamadas, errores;
        synchronized (s) {
            l = new ArrayList<>(s.lat);
            llamadas = s.llamadas;
            errores = s.errores;
        }
        l.sort(Long::compare);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("nombre", nombre);
        m.put("llamadas", llamadas);
        m.put("errores", errores);
        m.put("tasaError", llamadas == 0 ? 0 : Math.round(1000.0 * errores / llamadas) / 1000.0);
        m.put("p50Ms", percentil(l, 0.5));
        m.put("p95Ms", percentil(l, 0.95));
        m.put("maxMs", l.isEmpty() ? 0 : l.get(l.size() - 1));
        return m;
    }

    public List<Map<String, Object>> funcionalidades() {
        return series.entrySet().stream().sorted(Map.Entry.comparingByKey()).map(e -> resumen(e.getKey(), e.getValue())).toList();
    }

    public List<Map<String, Object>> pasos() {
        return pasos.entrySet().stream().map(e -> resumen(e.getKey(), e.getValue())).toList();
    }
}
