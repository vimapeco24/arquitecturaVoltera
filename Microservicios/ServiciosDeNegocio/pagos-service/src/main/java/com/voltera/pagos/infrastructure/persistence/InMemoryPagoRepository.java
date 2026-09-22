package com.voltera.pagos.infrastructure.persistence;

import com.voltera.pagos.domain.model.OrdenPago;
import com.voltera.pagos.domain.model.PagoId;
import com.voltera.pagos.domain.port.out.PagoRepositoryPort;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Repository
public class InMemoryPagoRepository implements PagoRepositoryPort {

    private final ConcurrentHashMap<String, OrdenPago> almacen = new ConcurrentHashMap<>();

    @Override
    public OrdenPago guardar(OrdenPago orden) {
        almacen.put(orden.id().valor(), orden);
        return orden;
    }

    @Override
    public Optional<OrdenPago> buscarPorId(PagoId id) {
        return Optional.ofNullable(almacen.get(id.valor()));
    }

    @Override
    public Optional<OrdenPago> buscarPorReferencia(String referencia) {
        return almacen.values().stream()
                .filter(o -> o.referencia().equals(referencia))
                .findFirst();
    }

    @Override
    public List<OrdenPago> buscarPorProsumidor(String prosumidorId) {
        return almacen.values().stream()
                .filter(o -> o.prosumidorId().equals(prosumidorId))
                .collect(Collectors.toList());
    }

    @Override
    public void eliminar(PagoId id) {
        almacen.remove(id.valor());
    }
}
