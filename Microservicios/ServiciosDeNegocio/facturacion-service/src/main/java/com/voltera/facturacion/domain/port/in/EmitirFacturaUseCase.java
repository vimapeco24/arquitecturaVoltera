package com.voltera.facturacion.domain.port.in;

import com.voltera.facturacion.domain.model.Factura;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Puerto de ENTRADA (driving port): caso de uso de emitir una factura.
 * Los adaptadores de entrada (REST) invocan esta interfaz.
 */
public interface EmitirFacturaUseCase {

    Factura emitir(ComandoEmitirFactura comando);

    Factura actualizar(String facturaId, ComandoEmitirFactura comando);

    void eliminar(String facturaId);

    /**
     * Comando inmutable con los datos necesarios para emitir una factura.
     * tarifaId/hora son OPCIONALES: si vienen, la facturación consulta el precio
     * vigente al servicio de Tarifas (salto real entre microservicios); si no,
     * usa el precioConsumoKwh recibido en el comando (comportamiento previo).
     */
    record ComandoEmitirFactura(
            String prosumidorId,
            LocalDate periodoInicio,
            LocalDate periodoFin,
            BigDecimal kwhConsumidos,
            BigDecimal kwhInyectados,
            BigDecimal precioConsumoKwh,
            BigDecimal precioExcedenteKwh,
            String tarifaId,
            Integer hora
    ) {
        /** Constructor de compatibilidad (sin consulta a tarifas). */
        public ComandoEmitirFactura(
                String prosumidorId,
                LocalDate periodoInicio,
                LocalDate periodoFin,
                BigDecimal kwhConsumidos,
                BigDecimal kwhInyectados,
                BigDecimal precioConsumoKwh,
                BigDecimal precioExcedenteKwh) {
            this(prosumidorId, periodoInicio, periodoFin, kwhConsumidos, kwhInyectados,
                    precioConsumoKwh, precioExcedenteKwh, null, null);
        }
    }
}
