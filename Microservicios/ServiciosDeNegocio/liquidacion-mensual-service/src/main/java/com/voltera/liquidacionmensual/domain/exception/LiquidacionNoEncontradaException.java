package com.voltera.liquidacionmensual.domain.exception;

public class LiquidacionNoEncontradaException extends RuntimeException {
    public LiquidacionNoEncontradaException(String id) {
        super("Liquidacion mensual no encontrada: " + id);
    }
}
