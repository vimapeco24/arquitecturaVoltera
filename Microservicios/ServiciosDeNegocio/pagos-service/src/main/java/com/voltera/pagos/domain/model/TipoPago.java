package com.voltera.pagos.domain.model;

/**
 * Tipo de operacion de pago.
 */
public enum TipoPago {
    COBRO_FACTURA,     // Voltera cobra al prosumidor
    PAGO_EXCEDENTE     // Voltera paga al prosumidor por su excedente
}
