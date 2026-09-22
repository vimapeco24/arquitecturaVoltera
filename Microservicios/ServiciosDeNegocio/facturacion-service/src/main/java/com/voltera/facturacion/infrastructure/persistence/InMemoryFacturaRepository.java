package com.voltera.facturacion.infrastructure.persistence;

import com.voltera.facturacion.domain.model.Factura;
import com.voltera.facturacion.domain.model.FacturaId;
import com.voltera.facturacion.domain.port.out.FacturaRepositoryPort;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Adaptador de SALIDA: implementacion en memoria del puerto de repositorio.
 * Elegida deliberadamente para MINIMIZAR el consumo de RAM: no arranca
 * un pool de conexiones ni un driver de BD, dejando la mayor parte de los
 * 5GB del pod disponibles para observabilidad y el sidecar del service mesh.
 *
 * Para produccion se sustituye por un adaptador JPA/JDBC SIN tocar el dominio.
 */
@Repository
public class InMemoryFacturaRepository implements FacturaRepositoryPort {

    private final ConcurrentHashMap<String, Factura> almacen = new ConcurrentHashMap<>();

    @Override
    public Factura guardar(Factura factura) {
        almacen.put(factura.id().valor(), factura);
        return factura;
    }

    @Override
    public Optional<Factura> buscarPorId(FacturaId id) {
        return Optional.ofNullable(almacen.get(id.valor()));
    }

    @Override
    public List<Factura> buscarPorProsumidor(String prosumidorId) {
        return almacen.values().stream()
                .filter(f -> f.prosumidorId().equals(prosumidorId))
                .collect(Collectors.toList());
    }

    @Override
    public void eliminar(FacturaId id) {
        almacen.remove(id.valor());
    }
}
