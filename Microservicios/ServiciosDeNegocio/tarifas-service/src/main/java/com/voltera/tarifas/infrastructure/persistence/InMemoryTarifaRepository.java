package com.voltera.tarifas.infrastructure.persistence;

import com.voltera.tarifas.domain.model.Tarifa;
import com.voltera.tarifas.domain.model.TarifaId;
import com.voltera.tarifas.domain.port.out.TarifaRepositoryPort;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** Adaptador por defecto (in-memory). Se desactiva con el perfil 'cqrs-jdbc'. */
@Repository
@Profile("!cqrs-jdbc")
public class InMemoryTarifaRepository implements TarifaRepositoryPort {

    private final ConcurrentHashMap<String, Tarifa> almacen = new ConcurrentHashMap<>();

    @Override
    public Tarifa guardar(Tarifa tarifa) {
        almacen.put(tarifa.id().valor(), tarifa);
        return tarifa;
    }

    @Override
    public Optional<Tarifa> buscarPorId(TarifaId id) {
        return Optional.ofNullable(almacen.get(id.valor()));
    }

    @Override
    public List<Tarifa> listar() {
        return new ArrayList<>(almacen.values());
    }

    @Override
    public void eliminar(TarifaId id) {
        almacen.remove(id.valor());
    }
}
