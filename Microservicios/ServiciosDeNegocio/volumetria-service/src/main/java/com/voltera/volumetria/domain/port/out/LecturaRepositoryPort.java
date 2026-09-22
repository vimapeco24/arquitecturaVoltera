package com.voltera.volumetria.domain.port.out;

import com.voltera.volumetria.domain.model.LecturaId;
import com.voltera.volumetria.domain.model.LecturaTelemetria;

import java.util.List;
import java.util.Optional;

/**
 * Puerto de SALIDA: persistencia de lecturas de telemetria.
 */
public interface LecturaRepositoryPort {

    LecturaTelemetria guardar(LecturaTelemetria lectura);

    Optional<LecturaTelemetria> buscarPorId(LecturaId id);

    List<LecturaTelemetria> buscarPorMedidor(String medidorId);

    void eliminar(LecturaId id);
}
