package com.voltera.reaseguro.domain.port.out;

import com.voltera.reaseguro.domain.model.CesionVista;

import java.util.List;
import java.util.Optional;

/**
 * Puerto de salida del modelo de LECTURA (read side / CQRS).
 */
public interface CesionVistaRepositoryPort {

    CesionVista guardar(CesionVista vista);

    Optional<CesionVista> buscarPorId(String id);

    List<CesionVista> buscarPorPoliza(String polizaId);

    List<CesionVista> buscarTodas();
}
