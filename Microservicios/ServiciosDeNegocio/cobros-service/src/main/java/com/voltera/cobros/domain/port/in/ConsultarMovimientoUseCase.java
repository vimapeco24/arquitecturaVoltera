package com.voltera.cobros.domain.port.in;

import com.voltera.cobros.domain.model.Movimiento;

import java.util.List;

/**
 * Puerto de ENTRADA (consulta): lectura de movimientos de cobro.
 */
public interface ConsultarMovimientoUseCase {

    Movimiento porId(String id);

    List<Movimiento> porPoliza(String polizaId);

    List<Movimiento> todos();
}
