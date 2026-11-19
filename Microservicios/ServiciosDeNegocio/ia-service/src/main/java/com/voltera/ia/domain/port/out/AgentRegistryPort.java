package com.voltera.ia.domain.port.out;

import java.util.List;

/**
 * Caja 4 · Agent Registry (A2A). Catálogo de los agentes y herramientas de la
 * organización: qué existe, quién es su dueño, qué puede hacer y con qué
 * permisos. Es la puerta para que un agente descubra y delegue en otro
 * (protocolo A2A) y el lugar donde se da de baja un agente.
 */
public interface AgentRegistryPort {

    /** Agente registrado: nombre, dueño, capacidad y permisos declarados. */
    record AgenteRegistrado(String nombre, String dueno, String capacidad,
                            String permisos, boolean activo) {}

    /** Lista los agentes activos del flujo de diagnóstico DER (en orden A2A). */
    List<AgenteRegistrado> agentesActivos();

    /** true si el agente existe y está habilitado para actuar. */
    boolean habilitado(String nombreAgente);

    /** Cambio de estado (alta/baja) de un agente, con responsable y motivo. */
    record CambioAgente(java.time.Instant instante, String agente, boolean activo, String responsable, String motivo) {}

    /** Todos los agentes registrados, activos o dados de baja. */
    default List<AgenteRegistrado> todos() { return agentesActivos(); }

    /** Da de alta o de baja un agente. */
    default AgenteRegistrado cambiarEstado(String nombre, boolean activo, String responsable, String motivo) {
        throw new UnsupportedOperationException("registro de solo lectura");
    }

    default List<CambioAgente> historial() { return List.of(); }
}
