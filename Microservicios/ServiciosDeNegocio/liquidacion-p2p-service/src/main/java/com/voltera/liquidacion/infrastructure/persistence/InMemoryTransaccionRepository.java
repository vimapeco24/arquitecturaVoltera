package com.voltera.liquidacion.infrastructure.persistence;

import com.voltera.liquidacion.domain.model.TransaccionId;
import com.voltera.liquidacion.domain.model.TransaccionP2P;
import com.voltera.liquidacion.domain.port.out.TransaccionRepositoryPort;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Adaptador de SALIDA: persistencia en memoria del puerto de transacciones.
 * Elegido para MINIMIZAR el consumo de RAM (sin driver ni pool de BD),
 * dejando margen para observabilidad y el sidecar del service mesh en el pod.
 * Se sustituye por JPA/JDBC sin tocar el dominio.
 */
@Repository
public class InMemoryTransaccionRepository implements TransaccionRepositoryPort {

    private final ConcurrentHashMap<String, TransaccionP2P> almacen = new ConcurrentHashMap<>();

    @Override
    public TransaccionP2P guardar(TransaccionP2P transaccion) {
        almacen.put(transaccion.id().valor(), transaccion);
        return transaccion;
    }

    @Override
    public Optional<TransaccionP2P> buscarPorId(TransaccionId id) {
        return Optional.ofNullable(almacen.get(id.valor()));
    }

    @Override
    public List<TransaccionP2P> buscarPorProsumidor(String prosumidorId) {
        return almacen.values().stream()
                .filter(t -> t.vendedorId().equals(prosumidorId)
                        || t.compradorId().equals(prosumidorId))
                .collect(Collectors.toList());
    }

    @Override
    public void eliminar(TransaccionId id) {
        almacen.remove(id.valor());
    }
}
