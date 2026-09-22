package com.voltera.siniestros.domain.exception;

public class SiniestroNoEncontradoException extends RuntimeException {
    public SiniestroNoEncontradoException(String id) {
        super("Siniestro no encontrado: " + id);
    }
}
