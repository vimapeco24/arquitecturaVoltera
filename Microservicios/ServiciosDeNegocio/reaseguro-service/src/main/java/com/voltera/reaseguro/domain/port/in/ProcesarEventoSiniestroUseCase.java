package com.voltera.reaseguro.domain.port.in;

import com.voltera.reaseguro.domain.event.SiniestroAprobado;
import com.voltera.reaseguro.domain.model.CesionRiesgo;

/**
 * Puerto de entrada del lado de ESCRITURA (comando): procesa el evento
 * {@code SiniestroAprobado} de forma idempotente por {@code eventId}.
 */
public interface ProcesarEventoSiniestroUseCase {

    /**
     * Procesa el evento. Si ya se proceso un evento con el mismo {@code eventId},
     * retorna la cesion existente sin duplicar.
     */
    CesionRiesgo procesar(SiniestroAprobado evento);
}
