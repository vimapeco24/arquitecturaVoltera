package com.voltera.cobros.domain.model;

/**
 * Estado del agregado {@link Movimiento} en el BC Cobros.
 *
 * <ul>
 *   <li>{@code PENDIENTE}: movimiento de cobro creado, a la espera del pago.</li>
 *   <li>{@code CONCILIADO}: el pago fue confirmado y se emitió el recibo.</li>
 * </ul>
 */
public enum EstadoMovimiento {
    PENDIENTE,
    CONCILIADO
}
