package com.voltera.liquidacion.domain.port.out;

import com.voltera.liquidacion.domain.model.TransaccionId;
import com.voltera.liquidacion.domain.model.TransaccionP2P;

import java.util.List;
import java.util.Optional;

/**
 * Puerto de SALIDA (driven port): persistencia de transacciones liquidadas.
 */
public interface TransaccionRepositoryPort {

    TransaccionP2P guardar(TransaccionP2P transaccion);

    Optional<TransaccionP2P> buscarPorId(TransaccionId id);

    List<TransaccionP2P> buscarPorProsumidor(String prosumidorId);

    void eliminar(TransaccionId id);
}
