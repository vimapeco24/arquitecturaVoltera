package com.voltera.liquidacion.domain.exception;

/**
 * Excepcion de dominio: la transaccion solicitada no existe.
 */
public class TransaccionNoEncontradaException extends RuntimeException {
    public TransaccionNoEncontradaException(String id) {
        super("Transaccion no encontrada: " + id);
    }
}
