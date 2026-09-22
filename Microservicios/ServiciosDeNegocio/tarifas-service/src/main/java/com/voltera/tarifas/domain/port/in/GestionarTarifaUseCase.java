package com.voltera.tarifas.domain.port.in;

import com.voltera.tarifas.domain.model.Tarifa;

import java.math.BigDecimal;
import java.util.List;

/**
 * Puerto de ENTRADA: crear tarifas y consultar el precio aplicable a una hora.
 */
public interface GestionarTarifaUseCase {

    Tarifa crear(ComandoCrearTarifa comando);

    Tarifa actualizar(String tarifaId, ComandoCrearTarifa comando);

    void eliminar(String tarifaId);

    BigDecimal precioEn(String tarifaId, int hora);

    record FranjaDTO(int horaInicio, int horaFin, BigDecimal precioKwh) {}

    record ComandoCrearTarifa(String nombre, List<FranjaDTO> franjas) {}
}
