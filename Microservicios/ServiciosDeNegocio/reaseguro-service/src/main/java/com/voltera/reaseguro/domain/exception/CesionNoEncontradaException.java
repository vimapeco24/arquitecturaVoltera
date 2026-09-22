package com.voltera.reaseguro.domain.exception;

/**
 * Se lanza cuando no se encuentra una cesion solicitada en el lado de lectura.
 */
public class CesionNoEncontradaException extends RuntimeException {
    public CesionNoEncontradaException(String mensaje) {
        super(mensaje);
    }
}
