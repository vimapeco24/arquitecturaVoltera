package com.voltera.telemetria.infrastructure.persistence;

import com.voltera.telemetria.domain.model.Medidor;
import com.voltera.telemetria.domain.model.MedidorId;
import com.voltera.telemetria.domain.port.out.MedidorRepositoryPort;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Repository
public class InMemoryMedidorRepository implements MedidorRepositoryPort {

    private final ConcurrentHashMap<String, Medidor> almacen = new ConcurrentHashMap<>();

    @Override
    public Medidor guardar(Medidor medidor) {
        almacen.put(medidor.id().valor(), medidor);
        return medidor;
    }

    @Override
    public Optional<Medidor> buscarPorId(MedidorId id) {
        return Optional.ofNullable(almacen.get(id.valor()));
    }

    @Override
    public List<Medidor> buscarPorProsumidor(String prosumidorId) {
        return almacen.values().stream()
                .filter(m -> m.prosumidorId().equals(prosumidorId))
                .collect(Collectors.toList());
    }

    @Override
    public List<Medidor> buscarTodos() {
        return List.copyOf(almacen.values());
    }

    @Override
    public void eliminar(MedidorId id) {
        almacen.remove(id.valor());
    }
}
