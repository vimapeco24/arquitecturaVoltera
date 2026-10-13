package com.voltera.cobros.domain.port.in;

import com.voltera.cobros.domain.event.PagoConfirmado;
import com.voltera.cobros.domain.model.Movimiento;

/**
 * Puerto de ENTRADA (reacción a evento): procesa {@code PagoConfirmado} y concilia
 * el movimiento de cobro correspondiente, de forma idempotente por {@code eventId}.
 */
public interface ProcesarPagoConfirmadoUseCase {

    /**
     * Concilia el movimiento asociado al pago. Si el evento ya fue procesado
     * (idempotencia) devuelve el movimiento existente sin volver a conciliar.
     */
    Movimiento procesar(PagoConfirmado evento);
}
