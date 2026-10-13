package com.voltera.cobros.domain.exception;

/**
 * Se lanza cuando no se encuentra un {@link com.voltera.cobros.domain.model.Movimiento}.
 */
public class MovimientoNoEncontradoException extends RuntimeException {
    public MovimientoNoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
