package com.voltera.pagos.domain.port.in;

import com.voltera.pagos.domain.model.OrdenPago;

import java.math.BigDecimal;

/**
 * Puerto de ENTRADA: crea y procesa una orden de pago (con idempotencia
 * por referencia externa).
 */
public interface ProcesarPagoUseCase {

    OrdenPago procesar(ComandoProcesarPago comando);

    OrdenPago actualizar(String pagoId, ComandoProcesarPago comando);

    void eliminar(String pagoId);

    record ComandoProcesarPago(
            String prosumidorId,
            String referencia,   // id de factura o transaccion
            String tipo,         // COBRO_FACTURA | PAGO_EXCEDENTE
            BigDecimal monto
    ) {}
}
