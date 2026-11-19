package com.voltera.ia.domain.exception;

/** Un agente requerido por el flujo fue dado de baja en el Agent Registry: el flujo no corre (fail-closed). */
public class AgenteDeBajaException extends RuntimeException {
    public AgenteDeBajaException(String agente) {
        super("El agente " + agente + " está dado de baja en el Agent Registry; el flujo no se ejecuta.");
    }
}
