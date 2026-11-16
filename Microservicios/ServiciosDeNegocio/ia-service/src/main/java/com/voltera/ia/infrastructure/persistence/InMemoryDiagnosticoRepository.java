package com.voltera.ia.infrastructure.persistence;

import com.voltera.ia.domain.model.der.DiagnosticoDer;
import com.voltera.ia.domain.port.out.DiagnosticoRepositoryPort;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * State &amp; Memory en memoria de los diagnósticos generados, para que el
 * Assistant y la gobernanza recuperen el caso por id.
 */
@Component
public class InMemoryDiagnosticoRepository implements DiagnosticoRepositoryPort {

    private final Map<UUID, DiagnosticoDer> store = new ConcurrentHashMap<>();

    @Override
    public DiagnosticoDer guardar(DiagnosticoDer diagnostico) {
        store.put(diagnostico.diagnosticoId(), diagnostico);
        return diagnostico;
    }

    @Override
    public Optional<DiagnosticoDer> porId(UUID diagnosticoId) {
        return Optional.ofNullable(store.get(diagnosticoId));
    }
}
