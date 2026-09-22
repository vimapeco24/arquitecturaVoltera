package com.voltera.tarifas.domain.port.in;

import com.voltera.tarifas.domain.model.Tarifa;

import java.util.List;

public interface ConsultarTarifaUseCase {
    Tarifa porId(String tarifaId);
    List<Tarifa> listar();
}
