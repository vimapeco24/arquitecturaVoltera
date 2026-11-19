package com.voltera.ia.infrastructure.revision;

import com.voltera.ia.domain.model.der.AlertaTelemetria;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * State &amp; Memory: guarda la alerta que originó cada diagnóstico (y cuándo se observó la
 * lectura) para que Human Review pueda convertir una corrección en un caso etiquetado
 * y para medir el tiempo de detección.
 */
@Component
public class RegistroAlertas {

    public record Entrada(AlertaTelemetria alerta, Instant observadoEn, Instant diagnosticadoEn) {}

    private static final int MAX = 2000;
    private final Map<UUID, Entrada> mapa = new LinkedHashMap<>(16, 0.75f, false) {
        @Override protected boolean removeEldestEntry(Map.Entry<UUID, Entrada> e) { return size() > MAX; }
    };

    public synchronized void guardar(UUID diagnosticoId, AlertaTelemetria a, Instant observadoEn, Instant diagnosticadoEn) {
        mapa.put(diagnosticoId, new Entrada(a, observadoEn, diagnosticadoEn));
    }

    public synchronized Optional<Entrada> porId(UUID diagnosticoId) {
        return Optional.ofNullable(mapa.get(diagnosticoId));
    }

    public synchronized java.util.List<Entrada> todas() {
        return java.util.List.copyOf(mapa.values());
    }
}
