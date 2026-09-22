package com.voltera.reaseguro.infrastructure.persistence;

import com.voltera.reaseguro.domain.model.CesionVista;
import com.voltera.reaseguro.domain.port.out.CesionVistaRepositoryPort;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Adaptador de persistencia en memoria para el modelo de LECTURA (CQRS).
 */
@Repository
public class InMemoryCesionVistaRepository implements CesionVistaRepositoryPort {

    private final ConcurrentHashMap<String, CesionVista> vistas = new ConcurrentHashMap<>();

    @Override
    public CesionVista guardar(CesionVista vista) {
        vistas.put(vista.id(), vista);
        return vista;
    }

    @Override
    public Optional<CesionVista> buscarPorId(String id) {
        return Optional.ofNullable(vistas.get(id));
    }

    @Override
    public List<CesionVista> buscarPorPoliza(String polizaId) {
        return vistas.values().stream()
                .filter(v -> v.polizaId() != null && v.polizaId().equals(polizaId))
                .toList();
    }

    @Override
    public List<CesionVista> buscarTodas() {
        return List.copyOf(vistas.values());
    }
}
