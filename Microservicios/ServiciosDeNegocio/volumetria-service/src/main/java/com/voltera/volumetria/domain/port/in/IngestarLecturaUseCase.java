package com.voltera.volumetria.domain.port.in;

import com.voltera.volumetria.domain.model.LecturaTelemetria;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Puerto de ENTRADA: ingesta y valida una lectura de telemetria.
 */
public interface IngestarLecturaUseCase {

    LecturaTelemetria ingestar(ComandoIngestarLectura comando);

    LecturaTelemetria actualizar(String lecturaId, ComandoIngestarLectura comando);

    void eliminar(String lecturaId);

    record ComandoIngestarLectura(
            String medidorId,
            BigDecimal kwh,
            String direccion,   // CONSUMO | GENERACION
            Instant capturadaEn
    ) {}
}
