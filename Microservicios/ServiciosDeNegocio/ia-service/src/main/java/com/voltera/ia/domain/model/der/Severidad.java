package com.voltera.ia.domain.model.der;

/**
 * Nivel de severidad del diagnóstico que produce el Agente-Diagnosticador.
 * Ordena la cola de atención y, junto con el tipo de acción, decide si la
 * acción correctiva exige aprobación humana.
 */
public enum Severidad {
    INFO,
    ADVERTENCIA,
    CRITICA
}
