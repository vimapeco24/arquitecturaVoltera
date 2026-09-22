package com.voltera.facturacion.domain.exception;

/**
 * Excepcion de dominio: la factura solicitada no existe.
 */
public class FacturaNoEncontradaException extends RuntimeException {
    public FacturaNoEncontradaException(String id) {
        super("Factura no encontrada: " + id);
    }
}
