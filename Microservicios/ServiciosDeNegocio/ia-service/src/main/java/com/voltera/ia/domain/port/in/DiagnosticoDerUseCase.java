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

    /** Ejecuta el flujo con una versión concreta del diagnosticador (Evals de versiones candidatas). */
    default DiagnosticoDer diagnosticar(AlertaTelemetria alerta, String versionDiagnosticador) {
        return diagnosticar(alerta);
    }
}
