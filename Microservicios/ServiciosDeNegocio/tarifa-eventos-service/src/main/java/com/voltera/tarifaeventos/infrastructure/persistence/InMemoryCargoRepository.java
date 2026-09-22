package com.voltera.tarifaeventos.infrastructure.persistence;

import com.voltera.tarifaeventos.domain.model.CargoId;
import com.voltera.tarifaeventos.domain.model.CargoTarifa;
import com.voltera.tarifaeventos.domain.port.out.CargoRepositoryPort;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Adaptador de persistencia en memoria para el modelo de ESCRITURA.
 * Mantiene ademas un indice por eventId para soportar la idempotencia.
 */
@Repository
public class InMemoryCargoRepository implements CargoRepositoryPort {

    private final ConcurrentHashMap<String, CargoTarifa> porId = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> eventIdToCargoId = new ConcurrentHashMap<>();

    @Override
    public CargoTarifa guardar(CargoTarifa cargo) {
        porId.put(cargo.getId().valor(), cargo);
        eventIdToCargoId.put(cargo.getEventId(), cargo.getId().valor());
        return cargo;
    }

    @Override
    public Optional<CargoTarifa> buscarPorId(CargoId id) {
        return Optional.ofNullable(porId.get(id.valor()));
    }

    @Override
    public Optional<CargoTarifa> buscarPorEventId(String eventId) {
        String cargoId = eventIdToCargoId.get(eventId);
        if (cargoId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(porId.get(cargoId));
    }

    @Override
    public List<CargoTarifa> buscarPorProsumidor(String prosumidorId) {
        return porId.values().stream()
                .filter(c -> c.getProsumidorId() != null && c.getProsumidorId().equals(prosumidorId))
                .toList();
    }

    @Override
    public List<CargoTarifa> buscarTodos() {
        return List.copyOf(porId.values());
    }
}
