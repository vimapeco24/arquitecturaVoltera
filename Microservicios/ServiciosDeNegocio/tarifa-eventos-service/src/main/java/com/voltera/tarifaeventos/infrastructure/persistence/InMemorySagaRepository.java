package com.voltera.tarifaeventos.infrastructure.persistence;

import com.voltera.tarifaeventos.domain.model.EstadoSaga;
import com.voltera.tarifaeventos.domain.model.SagaAltaMedidor;
import com.voltera.tarifaeventos.domain.port.out.SagaRepositoryPort;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Adaptador en memoria del repositorio de sagas de alta de medidor.
 */
@Repository
public class InMemorySagaRepository implements SagaRepositoryPort {

    private final ConcurrentHashMap<String, SagaAltaMedidor> porId = new ConcurrentHashMap<>();

    @Override
    public SagaAltaMedidor guardar(SagaAltaMedidor saga) {
        porId.put(saga.sagaId(), saga);
        return saga;
    }

    @Override
    public Optional<SagaAltaMedidor> buscarPorId(String sagaId) {
        return Optional.ofNullable(porId.get(sagaId));
    }

    @Override
    public Optional<SagaAltaMedidor> buscarPorMedidor(String medidorId) {
        return porId.values().stream()
                .filter(s -> s.medidorId().equals(medidorId))
                .reduce((primero, segundo) -> segundo); // la mas reciente
    }

    @Override
    public List<SagaAltaMedidor> pendientes() {
        return porId.values().stream()
                .filter(s -> s.estado() == EstadoSaga.INICIADA)
                .toList();
    }

    @Override
    public List<SagaAltaMedidor> todas() {
        return List.copyOf(porId.values());
    }
}
