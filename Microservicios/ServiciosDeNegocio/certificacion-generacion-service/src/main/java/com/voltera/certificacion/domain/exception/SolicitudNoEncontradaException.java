package com.voltera.certificacion.domain.exception;

public class SolicitudNoEncontradaException extends RuntimeException {
    public SolicitudNoEncontradaException(String id) {
        super("Solicitud de certificación no encontrada: " + id);
    }
}
