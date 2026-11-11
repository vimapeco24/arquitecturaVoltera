package com.voltera.ia.domain.port.out;

import com.voltera.ia.domain.model.IaRecomendacionGenerada;

/**
 * Puerto de salida: publica el evento IaRecomendacionGenerada hacia el broker
 * (EDA). Best-effort: devuelve false si el broker no confirma, sin romper el
 * flujo del copiloto.
 */
public interface EventPublisherPort {

    boolean publicar(IaRecomendacionGenerada evento);
}
