package com.voltera.tarifaeventos.domain.port.out;

import com.voltera.tarifaeventos.domain.model.CargoVista;

import java.util.List;
import java.util.Optional;

/**
 * Puerto de salida del modelo de LECTURA (read side). CQRS: las consultas se
 * sirven exclusivamente desde aqui.
 */
public interface CargoVistaRepositoryPort {

    CargoVista guardar(CargoVista vista);

    Optional<CargoVista> buscarPorId(String id);

    List<CargoVista> buscarPorProsumidor(String prosumidorId);

    List<CargoVista> buscarTodas();
}
