package com.voltera.ia.domain.port.out;

import com.voltera.ia.domain.model.der.DiagnosticoDerGenerado;

/**
 * Puerto de salida: publica el evento DiagnosticoDerGenerado hacia el broker
 * (EDA). Best-effort: devuelve false si el broker no confirma (en la nube cae al
 * publicador de log).
 */
public interface DiagnosticoEventPublisherPort {

    boolean publicar(DiagnosticoDerGenerado evento);
}
