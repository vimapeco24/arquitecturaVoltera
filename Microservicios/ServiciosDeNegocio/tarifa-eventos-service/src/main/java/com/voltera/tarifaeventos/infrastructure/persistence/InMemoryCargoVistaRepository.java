package com.voltera.tarifaeventos.infrastructure.persistence;

import com.voltera.tarifaeventos.domain.model.CargoVista;
import com.voltera.tarifaeventos.domain.port.out.CargoVistaRepositoryPort;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Adaptador de persistencia en memoria para el modelo de LECTURA (CQRS).
 */
@Repository
public class InMemoryCargoVistaRepository implements CargoVistaRepositoryPort {

    private final ConcurrentHashMap<String, CargoVista> vistas = new ConcurrentHashMap<>();

    @Override
    public CargoVista guardar(CargoVista vista) {
        vistas.put(vista.id(), vista);
        return vista;
    }

    @Override
    public Optional<CargoVista> buscarPorId(String id) {
        return Optional.ofNullable(vistas.get(id));
    }

    @Override
    public List<CargoVista> buscarPorProsumidor(String prosumidorId) {
        return vistas.values().stream()
                .filter(v -> v.prosumidorId() != null && v.prosumidorId().equals(prosumidorId))
                .toList();
    }

    @Override
    public List<CargoVista> buscarTodas() {
        return List.copyOf(vistas.values());
    }
}
