package com.voltera.blockchain.infrastructure.persistence;

import com.voltera.blockchain.domain.model.Anclaje;
import com.voltera.blockchain.domain.port.out.AnclajeRepositoryPort;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Repositorio del OUTBOX en memoria (igual criterio que el resto de servicios de
 * la Entrega, que usan persistencia en memoria). Clave = transaccionId.
 */
public class InMemoryAnclajeRepository implements AnclajeRepositoryPort {

    private final ConcurrentMap<String, Anclaje> datos = new ConcurrentHashMap<>();

    @Override
    public Anclaje guardar(Anclaje anclaje) {
        datos.put(anclaje.transaccionId(), anclaje);
        return anclaje;
    }

    @Override
    public Optional<Anclaje> buscarPorTransaccion(String transaccionId) {
        return Optional.ofNullable(datos.get(transaccionId));
    }

    @Override
    public List<Anclaje> buscarPendientes() {
        List<Anclaje> r = new ArrayList<>();
        for (Anclaje a : datos.values()) {
            if (a.estaPendiente()) r.add(a);
        }
        return r;
    }

    @Override
    public List<Anclaje> buscarTodos() {
        return new ArrayList<>(datos.values());
    }
}
