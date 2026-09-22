package com.voltera.volumetria.infrastructure.persistence;

import com.voltera.volumetria.domain.model.LecturaId;
import com.voltera.volumetria.domain.model.LecturaTelemetria;
import com.voltera.volumetria.domain.port.out.LecturaRepositoryPort;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Adaptador de SALIDA en memoria (bajo consumo de RAM).
 */
@Repository
public class InMemoryLecturaRepository implements LecturaRepositoryPort {

    private final ConcurrentHashMap<String, LecturaTelemetria> almacen = new ConcurrentHashMap<>();

    @Override
    public LecturaTelemetria guardar(LecturaTelemetria lectura) {
        almacen.put(lectura.id().valor(), lectura);
        return lectura;
    }

    @Override
    public Optional<LecturaTelemetria> buscarPorId(LecturaId id) {
        return Optional.ofNullable(almacen.get(id.valor()));
    }

    @Override
    public List<LecturaTelemetria> buscarPorMedidor(String medidorId) {
        return almacen.values().stream()
                .filter(l -> l.medidorId().valor().equals(medidorId))
                .collect(Collectors.toList());
    }

    @Override
    public void eliminar(LecturaId id) {
        almacen.remove(id.valor());
    }
}
