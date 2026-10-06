package com.voltera.habilitacion.domain.exception;

public class MedidorNoEncontradoException extends RuntimeException {
    public MedidorNoEncontradoException(String medidorId) {
        super("No se encontro el medidor: " + medidorId);
    }
}
