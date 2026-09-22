package com.voltera.facturacion.domain.port.in;

import com.voltera.facturacion.domain.model.Factura;

import java.util.List;

/**
 * Puerto de ENTRADA (driving port): casos de uso de consulta de facturas.
 */
public interface ConsultarFacturaUseCase {

    Factura porId(String facturaId);

    List<Factura> porProsumidor(String prosumidorId);
}
