package com.voltera.telemetria.domain.port.out;

import com.voltera.telemetria.domain.model.Medidor;
import com.voltera.telemetria.domain.model.MedidorId;

import java.util.List;
import java.util.Optional;

public interface MedidorRepositoryPort {
    Medidor guardar(Medidor medidor);
    Optional<Medidor> buscarPorId(MedidorId id);
    List<Medidor> buscarPorProsumidor(String prosumidorId);
    List<Medidor> buscarTodos();
    void eliminar(MedidorId id);
}
