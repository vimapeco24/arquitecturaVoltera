package com.voltera.cobros.domain.port.out;

import com.voltera.cobros.domain.model.Movimiento;

import java.util.List;
import java.util.Optional;

/**
 * Puerto de SALIDA: persistencia del agregado {@link Movimiento}.
 */
public interface MovimientoRepositoryPort {

    Movimiento guardar(Movimiento movimiento);

    Optional<Movimiento> buscarPorId(String id);

    /** Busca el movimiento PENDIENTE de una póliza para conciliarlo con el pago. */
    Optional<Movimiento> buscarPendientePorPoliza(String polizaId);

    List<Movimiento> buscarPorPoliza(String polizaId);

    List<Movimiento> buscarTodos();
}
