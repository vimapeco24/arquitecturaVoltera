package com.voltera.telemetriacore.infrastructure.persistence;

import com.voltera.telemetriacore.domain.model.SerieDeMedicion;
import com.voltera.telemetriacore.domain.port.out.SerieRepositoryPort;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** Adaptador por defecto (in-memory) de la serie append-only. Se desactiva con el perfil 'tsdb'. */
@Repository
@Profile("!tsdb")
public class InMemorySerieRepository implements SerieRepositoryPort {

    private final ConcurrentHashMap<String, SerieDeMedicion> porMedidor = new ConcurrentHashMap<>();

    @Override
    public SerieDeMedicion guardar(SerieDeMedicion serie) {
        porMedidor.put(serie.medidorSerial(), serie);
        return serie;
    }

    @Override
    public Optional<SerieDeMedicion> buscarPorMedidor(String medidorSerial) {
        return Optional.ofNullable(porMedidor.get(medidorSerial));
    }

    @Override
    public List<SerieDeMedicion> todas() {
        return List.copyOf(porMedidor.values());
    }
}
