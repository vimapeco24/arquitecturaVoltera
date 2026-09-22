package com.voltera.siniestros.infrastructure.persistence;

import com.voltera.siniestros.domain.model.Siniestro;
import com.voltera.siniestros.domain.model.SiniestroId;
import com.voltera.siniestros.domain.port.out.SiniestroRepositoryPort;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Repository
public class InMemorySiniestroRepository implements SiniestroRepositoryPort {

    private final ConcurrentHashMap<String, Siniestro> almacen = new ConcurrentHashMap<>();

    @Override
    public Siniestro guardar(Siniestro siniestro) {
        almacen.put(siniestro.id().valor(), siniestro);
        return siniestro;
    }

    @Override
    public Optional<Siniestro> buscarPorId(SiniestroId id) {
        return Optional.ofNullable(almacen.get(id.valor()));
    }

    @Override
    public List<Siniestro> buscarPorPoliza(String polizaId) {
        return almacen.values().stream()
                .filter(s -> s.polizaId().equals(polizaId))
                .collect(Collectors.toList());
    }

    @Override
    public List<Siniestro> buscarTodos() {
        return List.copyOf(almacen.values());
    }

    @Override
    public void eliminar(SiniestroId id) {
        almacen.remove(id.valor());
    }
}
