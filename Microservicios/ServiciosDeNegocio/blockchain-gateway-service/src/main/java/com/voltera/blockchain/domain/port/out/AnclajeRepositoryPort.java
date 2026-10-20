package com.voltera.blockchain.domain.port.out;

import com.voltera.blockchain.domain.model.Anclaje;

import java.util.List;
import java.util.Optional;

/** Puerto de salida: persistencia del OUTBOX de anclajes. */
public interface AnclajeRepositoryPort {
    Anclaje guardar(Anclaje anclaje);
    Optional<Anclaje> buscarPorTransaccion(String transaccionId);
    List<Anclaje> buscarPendientes();
    List<Anclaje> buscarTodos();
}
