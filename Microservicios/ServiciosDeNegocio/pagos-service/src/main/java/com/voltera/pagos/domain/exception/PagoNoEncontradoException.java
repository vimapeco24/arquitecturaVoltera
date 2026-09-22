package com.voltera.pagos.domain.exception;

public class PagoNoEncontradoException extends RuntimeException {
    public PagoNoEncontradoException(String id) {
        super("Orden de pago no encontrada: " + id);
    }
}
