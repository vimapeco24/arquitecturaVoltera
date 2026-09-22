package com.voltera.tarifaeventos.domain.exception;

public class CargoNoEncontradoException extends RuntimeException {
    public CargoNoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
