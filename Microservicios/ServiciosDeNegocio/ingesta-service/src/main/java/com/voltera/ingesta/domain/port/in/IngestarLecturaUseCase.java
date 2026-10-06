package com.voltera.ingesta.domain.port.in;

import com.voltera.ingesta.domain.event.LecturaCrudaRecibida;
import com.voltera.ingesta.domain.model.ResultadoValidacion;
import com.voltera.ingesta.domain.model.SesionDeIngesta;

/**
 * Puerto de ENTRADA: MS Ingesta de telemetria (BC Telemetria · Ingesta, lamina 02).
 */
public interface IngestarLecturaUseCase {

    /** Procesa una LecturaCrudaRecibida: valida/deduplica y emite el evento resultante. */
    ResultadoValidacion procesar(LecturaCrudaRecibida lectura);

    /** Reaccion a MedidorHabilitado: abre el canal y emite CanalIngestaCreado la primera vez. */
    void abrirCanal(String medidorSerial);

    /** Reaccion a MedidorSuspendido: cierra el canal. */
    void cerrarCanal(String medidorSerial);

    SesionDeIngesta estado();
}
