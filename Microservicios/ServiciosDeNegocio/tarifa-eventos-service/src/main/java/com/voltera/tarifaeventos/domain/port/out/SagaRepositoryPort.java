package com.voltera.tarifaeventos.domain.port.out;

import com.voltera.tarifaeventos.domain.model.SagaAltaMedidor;

import java.util.List;
import java.util.Optional;

/**
 * Puerto de SALIDA: persistencia de las sagas de alta de medidor (process manager).
 */
public interface SagaRepositoryPort {

    SagaAltaMedidor guardar(SagaAltaMedidor saga);

    Optional<SagaAltaMedidor> buscarPorId(String sagaId);

    Optional<SagaAltaMedidor> buscarPorMedidor(String medidorId);

    /** Sagas aun en estado INICIADA (candidatas a timeout). */
    List<SagaAltaMedidor> pendientes();

    List<SagaAltaMedidor> todas();
}
