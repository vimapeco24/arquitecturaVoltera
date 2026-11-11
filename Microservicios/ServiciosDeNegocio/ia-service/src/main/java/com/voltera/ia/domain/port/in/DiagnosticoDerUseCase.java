package com.voltera.ia.domain.port.in;

import com.voltera.ia.domain.model.der.AlertaTelemetria;
import com.voltera.ia.domain.model.der.DiagnosticoDer;

/**
 * Puerto de entrada del caso del Grupo 5: "Agente de diagnóstico de activos
 * distribuidos (DER) y copiloto de soporte de red". Ejecuta el flujo de tres
 * agentes (Monitor → Diagnosticador → Acción) sobre una alerta de telemetría y
 * devuelve un diagnóstico con la acción propuesta.
 */
public interface DiagnosticoDerUseCase {

    DiagnosticoDer diagnosticar(AlertaTelemetria alerta);
}
