package com.voltera.facturacion.domain.exception;

/**
 * Se lanza cuando el servicio de Tarifas no está disponible y el Circuit Breaker
 * de la llamada facturacion -> tarifas se abre (fail-fast). El adaptador la
 * produce desde su método de fallback; el borde REST la traduce a HTTP 503.
 */
public class TarifaNoDisponibleException extends RuntimeException {

    public TarifaNoDisponibleException(String tarifaId, Throwable causa) {
        super("Servicio de Tarifas no disponible (circuit breaker) para tarifa=" + tarifaId, causa);
    }
}
