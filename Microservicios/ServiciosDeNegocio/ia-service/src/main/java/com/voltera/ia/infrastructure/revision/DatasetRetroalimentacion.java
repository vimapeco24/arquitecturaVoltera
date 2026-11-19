package com.voltera.ia.infrastructure.revision;

import com.voltera.ia.domain.model.evals.CasoEval;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Human Review → Evals: cada revisión de un diagnóstico (de acuerdo o corregida) queda
 * como un caso etiquetado que entra a la siguiente corrida de Evals. Es la forma más
 * barata de mejorar el dataset con casos reales.
 */
@Component
public class DatasetRetroalimentacion {

    /** Revisión humana de un diagnóstico. */
    public record Revision(String id, Instant instante, String diagnosticoId, String revisor,
                           String severidadAgente, String severidadCorrecta,
                           String accionAgente, String accionCorrecta, boolean deAcuerdo,
                           String comentario, String casoId) {}

    private final List<Revision> revisiones = Collections.synchronizedList(new ArrayList<>());
    private final List<CasoEval> casos = Collections.synchronizedList(new ArrayList<>());

    public Revision agregar(Revision r, CasoEval caso) {
        synchronized (revisiones) {
            revisiones.add(r);
            casos.add(caso);
        }
        return r;
    }

    public List<CasoEval> casos() {
        synchronized (casos) { return List.copyOf(casos); }
    }

    public int siguienteNumero() {
        return casos.size() + 1;
    }

    public List<Revision> revisiones() {
        synchronized (revisiones) {
            List<Revision> out = new ArrayList<>(revisiones);
            Collections.reverse(out);
            return out;
        }
    }

    public Map<String, Object> resumen() {
        List<Revision> rs = revisiones();
        long acuerdo = rs.stream().filter(Revision::deAcuerdo).count();
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("revisiones", rs.size());
        m.put("deAcuerdo", acuerdo);
        m.put("corregidas", rs.size() - acuerdo);
        m.put("tasaAcuerdo", rs.isEmpty() ? 0 : Math.round(1000.0 * acuerdo / rs.size()) / 1000.0);
        m.put("casosAgregadosAEvals", casos().size());
        return m;
    }
}
