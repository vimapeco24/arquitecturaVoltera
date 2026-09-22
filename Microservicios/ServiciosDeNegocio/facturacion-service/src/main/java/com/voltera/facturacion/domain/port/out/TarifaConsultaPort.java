package com.voltera.facturacion.domain.port.out;

import java.math.BigDecimal;

/**
 * Puerto de SALIDA (driven port): el dominio de Facturación necesita conocer el
 * precio vigente de una tarifa para calcular la factura, SIN saber que ese dato
 * vive en otro microservicio (tarifas-service). Un adaptador de infraestructura
 * implementa este puerto realizando la llamada real entre microservicios.
 */
public interface TarifaConsultaPort {

    /**
     * Obtiene el precio por kWh de una tarifa en una hora dada.
     *
     * @param tarifaId identificador de la tarifa en el servicio de Tarifas
     * @param hora     hora del día [0..23]
     * @return precio por kWh
     */
    BigDecimal obtenerPrecioKwh(String tarifaId, int hora);
}
