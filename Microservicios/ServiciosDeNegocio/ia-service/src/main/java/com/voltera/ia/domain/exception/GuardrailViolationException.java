package com.voltera.ia.domain.exception;

/**
 * Se lanza cuando un guardrail de entrada rechaza la solicitud (p. ej. intento
 * de inyeccion de prompt o tema fuera de alcance).
 */
public class GuardrailViolationException extends RuntimeException {
    public GuardrailViolationException(String mensaje) {
        super(mensaje);
    }
}
