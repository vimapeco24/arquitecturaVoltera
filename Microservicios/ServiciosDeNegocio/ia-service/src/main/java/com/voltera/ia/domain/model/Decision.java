package com.voltera.ia.domain.model;

/**
 * Accion que el copiloto PROPONE. El agente nunca decide lo que compromete a la
 * empresa: propone, y una persona aprueba/rechaza (Responsible AI).
 */
public enum Decision {
    APROBAR,
    RECHAZAR,
    SOLICITAR_INFO,
    ESCALAR_HUMANO
}
