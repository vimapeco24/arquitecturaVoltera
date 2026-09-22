package com.voltera.tarifaeventos.domain.port.out;

import com.voltera.tarifaeventos.domain.model.CargoId;
import com.voltera.tarifaeventos.domain.model.CargoTarifa;

import java.util.List;
import java.util.Optional;

/**
 * Puerto de salida del modelo de ESCRITURA (write side).
 */
public interface CargoRepositoryPort {

    CargoTarifa guardar(CargoTarifa cargo);

    Optional<CargoTarifa> buscarPorId(CargoId id);

    /** Clave de idempotencia: busca un cargo ya creado para un eventId. */
    Optional<CargoTarifa> buscarPorEventId(String eventId);

    List<CargoTarifa> buscarPorProsumidor(String prosumidorId);

    List<CargoTarifa> buscarTodos();
}
