package com.voltera.tarifaeventos.domain.exception;

/**
 * Se lanza cuando no existe la saga de alta solicitada.
 */
public class SagaNoEncontradaException extends RuntimeException {
    public SagaNoEncontradaException(String sagaId) {
        super("No se encontro la saga de alta: " + sagaId);
    }
}
