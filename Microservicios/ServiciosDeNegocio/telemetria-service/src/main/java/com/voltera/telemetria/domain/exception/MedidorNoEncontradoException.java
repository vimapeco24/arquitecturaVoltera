package com.voltera.telemetria.domain.exception;

public class MedidorNoEncontradoException extends RuntimeException {
    public MedidorNoEncontradoException(String medidorId) {
        super("No se encontro el medidor con id: " + medidorId);
    }
}
