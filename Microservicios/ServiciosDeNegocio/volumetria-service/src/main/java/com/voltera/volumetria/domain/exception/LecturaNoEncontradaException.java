package com.voltera.volumetria.domain.exception;

public class LecturaNoEncontradaException extends RuntimeException {
    public LecturaNoEncontradaException(String id) {
        super("Lectura no encontrada: " + id);
    }
}
