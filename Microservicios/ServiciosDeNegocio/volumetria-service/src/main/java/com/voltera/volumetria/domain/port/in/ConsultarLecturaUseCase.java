package com.voltera.volumetria.domain.port.in;

import com.voltera.volumetria.domain.model.LecturaTelemetria;

import java.util.List;

/**
 * Puerto de ENTRADA: consultas de lecturas.
 */
public interface ConsultarLecturaUseCase {

    LecturaTelemetria porId(String lecturaId);

    List<LecturaTelemetria> porMedidor(String medidorId);
}
