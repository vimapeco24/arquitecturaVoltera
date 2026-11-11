package com.voltera.ia.domain.port.out;

import com.voltera.ia.domain.model.Evidencia;
import com.voltera.ia.domain.model.der.AlertaTelemetria;

import java.util.List;

/**
 * Caja 3 · MCP Servers. Expone la capa de gestión de dispositivos DER
 * (telemetría en tiempo real, historial de comandos, topología de red) como
 * herramientas para los agentes, con autorización firmada y verificación Zero
 * Trust por dispositivo. El agente actúa con la identidad del usuario (IAM) y
 * con mínimo privilegio: lee telemetría/historial/topología; NO ejecuta comandos
 * físicos (esos los aprueba una persona).
 */
public interface HerramientasDerPort {

    List<Evidencia> consultar(AlertaTelemetria alerta);
}
