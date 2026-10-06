package com.voltera.habilitacion.infrastructure.persistence;

import com.voltera.habilitacion.domain.model.EstadoHabilitacion;
import com.voltera.habilitacion.domain.model.Medidor;
import com.voltera.habilitacion.domain.port.out.MedidorRepositoryPort;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryMedidorRepository implements MedidorRepositoryPort {

    private final ConcurrentHashMap<String, Medidor> porId = new ConcurrentHashMap<>();

    @Override
    public Medidor guardar(Medidor medidor) {
        porId.put(medidor.medidorId(), medidor);
        return medidor;
    }

    @Override
    public Optional<Medidor> buscarPorId(String medidorId) {
        return Optional.ofNullable(porId.get(medidorId));
    }

    @Override
    public List<Medidor> pendientes() {
        return porId.values().stream()
                .filter(m -> m.estado() == EstadoHabilitacion.PENDIENTE)
                .toList();
    }

    @Override
    public List<Medidor> todos() {
        return List.copyOf(porId.values());
    }
}
