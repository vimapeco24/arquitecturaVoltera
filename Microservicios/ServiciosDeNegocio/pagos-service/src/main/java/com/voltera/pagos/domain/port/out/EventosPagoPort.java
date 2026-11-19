package com.voltera.pagos.domain.port.out;

import com.voltera.pagos.domain.model.OrdenPago;

/** Puerto de salida para anunciar hechos de dominio de Pagos. */
public interface EventosPagoPort {
    /** Anuncia que una orden de pago fue confirmada (PROCESADO). */
    void pagoConfirmado(OrdenPago orden);
}
