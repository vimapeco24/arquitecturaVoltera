package com.voltera.habilitacion.domain.port.out;

import com.voltera.habilitacion.domain.model.Medidor;

import java.util.List;
import java.util.Optional;

/** Puerto de SALIDA: persistencia del agregado Medidor. */
public interface MedidorRepositoryPort {
    Medidor guardar(Medidor medidor);
    Optional<Medidor> buscarPorId(String medidorId);
    Optional<Medidor> buscarPorSerial(String serial);
    List<Medidor> pendientes();
    List<Medidor> todos();
}
