package com.voltera.cobros.domain.port.in;

import com.voltera.cobros.domain.model.Movimiento;
import com.voltera.cobros.domain.model.Prima;

/**
 * Puerto de ENTRADA (comando): registra un nuevo movimiento de cobro de prima.
 */
public interface RegistrarMovimientoUseCase {

    Movimiento registrar(ComandoRegistrarMovimiento comando);

    record ComandoRegistrarMovimiento(String clienteId, String polizaId, Prima prima) {}
}
