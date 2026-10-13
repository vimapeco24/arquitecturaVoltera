package com.voltera.cobros.domain.model;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Entidad <b>Recibo</b> del BC Cobros (lámina 02).
 *
 * <p>Representa el comprobante de un cobro conciliado contra un pago confirmado.
 * Se genera cuando el movimiento se concilia con el evento {@code PagoConfirmado}.</p>
 *
 * @param numero      número de recibo (identidad dentro del movimiento)
 * @param referencia  referencia del pago confirmado (idempotencia de conciliación)
 * @param montoPagado importe efectivamente pagado
 * @param emitidoEn   instante de emisión del recibo
 */
public record Recibo(String numero, String referencia, BigDecimal montoPagado, Instant emitidoEn) {

    public Recibo {
        if (numero == null || numero.isBlank()) {
            throw new IllegalArgumentException("El número de recibo es obligatorio");
        }
        if (montoPagado == null || montoPagado.signum() < 0) {
            throw new IllegalArgumentException("El monto pagado no puede ser negativo");
        }
        if (emitidoEn == null) {
            emitidoEn = Instant.now();
        }
    }

    public static Recibo emitir(String referencia, BigDecimal montoPagado) {
        return new Recibo("REC-" + System.currentTimeMillis(), referencia, montoPagado, Instant.now());
    }
}
