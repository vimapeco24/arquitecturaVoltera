package com.voltera.telemetria.domain.port.in;

import com.voltera.telemetria.domain.model.Medidor;

import java.util.List;

public interface ConsultarMedidorUseCase {
    Medidor porId(String medidorId);
    List<Medidor> porProsumidor(String prosumidorId);
    List<Medidor> todos();
}
