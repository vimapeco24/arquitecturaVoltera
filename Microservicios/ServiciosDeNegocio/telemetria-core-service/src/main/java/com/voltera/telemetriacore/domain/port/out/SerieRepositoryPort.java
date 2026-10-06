package com.voltera.telemetriacore.domain.port.out;

import com.voltera.telemetriacore.domain.model.SerieDeMedicion;

import java.util.List;
import java.util.Optional;

/** Puerto de SALIDA (lado COMMAND): persistencia de las series por medidor. */
public interface SerieRepositoryPort {
    SerieDeMedicion guardar(SerieDeMedicion serie);
    Optional<SerieDeMedicion> buscarPorMedidor(String medidorSerial);
    List<SerieDeMedicion> todas();
}
