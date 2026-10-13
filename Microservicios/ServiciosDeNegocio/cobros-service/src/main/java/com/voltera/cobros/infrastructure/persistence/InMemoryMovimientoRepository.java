package com.voltera.cobros.infrastructure.persistence;

import com.voltera.cobros.domain.model.EstadoMovimiento;
import com.voltera.cobros.domain.model.Movimiento;
import com.voltera.cobros.domain.port.out.MovimientoRepositoryPort;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Adaptador en memoria del repositorio de {@link Movimiento}.
 */
@Repository
public class InMemoryMovimientoRepository implements MovimientoRepositoryPort {

    private final ConcurrentHashMap<String, Movimiento> datos = new ConcurrentHashMap<>();

    @Override
    public Movimiento guardar(Movimiento movimiento) {
        datos.put(movimiento.id().valor(), movimiento);
        return movimiento;
    }

    @Override
    public Optional<Movimiento> buscarPorId(String id) {
        return Optional.ofNullable(datos.get(id));
    }

    @Override
    public Optional<Movimiento> buscarPendientePorPoliza(String polizaId) {
        return datos.values().stream()
                .filter(m -> m.polizaId().equals(polizaId))
                .filter(m -> m.estado() == EstadoMovimiento.PENDIENTE)
                .findFirst();
    }

    @Override
    public List<Movimiento> buscarPorPoliza(String polizaId) {
        return datos.values().stream()
                .filter(m -> m.polizaId().equals(polizaId))
                .toList();
    }

    @Override
    public List<Movimiento> buscarTodos() {
        return List.copyOf(datos.values());
    }
}
