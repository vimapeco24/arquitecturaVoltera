package com.voltera.tarifas.domain.exception;

public class TarifaNoEncontradaException extends RuntimeException {
    public TarifaNoEncontradaException(String id) {
        super("Tarifa no encontrada: " + id);
    }
}
