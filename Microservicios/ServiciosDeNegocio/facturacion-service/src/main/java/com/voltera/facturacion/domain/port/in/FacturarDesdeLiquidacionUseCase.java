package com.voltera.facturacion.domain.port.in;

/**
 * Puerto de ENTRADA del flujo EDA de Facturacion (diagrama DDD 02).
 * Ante {@code LiquidacionCalculada} emite la factura del periodo y publica
 * {@code FacturaEmitida}.
 */
public interface FacturarDesdeLiquidacionUseCase {

    void facturarDesdeLiquidacion(String liquidacionId, String medidorSerial,
                                  String periodo, double consumoTotalKwh);
}
