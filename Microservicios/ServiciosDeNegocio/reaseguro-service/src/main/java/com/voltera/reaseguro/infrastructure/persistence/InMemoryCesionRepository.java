package com.voltera.reaseguro.infrastructure.persistence;

import com.voltera.reaseguro.domain.model.CesionId;
import com.voltera.reaseguro.domain.model.CesionRiesgo;
import com.voltera.reaseguro.domain.port.out.CesionRepositoryPort;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Adaptador de persistencia en memoria para el modelo de ESCRITURA.
 * Mantiene ademas un indice por eventId para soportar la idempotencia.
 */
@Repository
public class InMemoryCesionRepository implements CesionRepositoryPort {

    private final ConcurrentHashMap<String, CesionRiesgo> porId = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> eventIdToCesionId = new ConcurrentHashMap<>();

    @Override
    public CesionRiesgo guardar(CesionRiesgo cesion) {
        porId.put(cesion.getId().valor(), cesion);
        eventIdToCesionId.put(cesion.getEventId(), cesion.getId().valor());
        return cesion;
    }

    @Override
    public Optional<CesionRiesgo> buscarPorId(CesionId id) {
        return Optional.ofNullable(porId.get(id.valor()));
    }

    @Override
    public Optional<CesionRiesgo> buscarPorEventId(String eventId) {
        String cesionId = eventIdToCesionId.get(eventId);
        if (cesionId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(porId.get(cesionId));
    }

    @Override
    public List<CesionRiesgo> buscarPorPoliza(String polizaId) {
        return porId.values().stream()
                .filter(c -> c.getPolizaId() != null && c.getPolizaId().equals(polizaId))
                .toList();
    }

    @Override
    public List<CesionRiesgo> buscarTodas() {
        return List.copyOf(porId.values());
    }
}
