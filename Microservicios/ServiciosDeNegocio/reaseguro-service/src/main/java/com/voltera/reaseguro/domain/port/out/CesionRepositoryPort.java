package com.voltera.reaseguro.domain.port.out;

import com.voltera.reaseguro.domain.model.CesionId;
import com.voltera.reaseguro.domain.model.CesionRiesgo;

import java.util.List;
import java.util.Optional;

/**
 * Puerto de salida del modelo de ESCRITURA (write side).
 */
public interface CesionRepositoryPort {

    CesionRiesgo guardar(CesionRiesgo cesion);

    Optional<CesionRiesgo> buscarPorId(CesionId id);

    /** Clave de idempotencia: busca una cesion ya creada para un eventId. */
    Optional<CesionRiesgo> buscarPorEventId(String eventId);

    List<CesionRiesgo> buscarPorPoliza(String polizaId);

    List<CesionRiesgo> buscarTodas();
}
