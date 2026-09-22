package com.voltera.tarifaeventos.domain.port.in;

import com.voltera.tarifaeventos.domain.model.CargoVista;

import java.util.List;

/**
 * Puerto de ENTRADA: consultas del lado de LECTURA (CQRS).
 */
public interface ConsultarCargoUseCase {
    CargoVista porId(String id);
    List<CargoVista> porProsumidor(String prosumidorId);
    List<CargoVista> todos();
}
