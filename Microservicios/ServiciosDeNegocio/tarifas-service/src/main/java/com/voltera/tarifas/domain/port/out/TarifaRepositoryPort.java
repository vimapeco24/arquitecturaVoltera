package com.voltera.tarifas.domain.port.out;

import com.voltera.tarifas.domain.model.Tarifa;
import com.voltera.tarifas.domain.model.TarifaId;

import java.util.List;
import java.util.Optional;

public interface TarifaRepositoryPort {
    Tarifa guardar(Tarifa tarifa);
    Optional<Tarifa> buscarPorId(TarifaId id);
    List<Tarifa> listar();
    void eliminar(TarifaId id);
}
