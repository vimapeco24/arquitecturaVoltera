package com.voltera.liquidacionmensual.infrastructure.persistence;

import com.voltera.liquidacionmensual.domain.model.LiquidacionId;
import com.voltera.liquidacionmensual.domain.model.LiquidacionMensual;
import com.voltera.liquidacionmensual.domain.port.out.LiquidacionRepositoryPort;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Adaptador de SALIDA en memoria (bajo consumo de RAM).
 */
@Repository
public class InMemoryLiquidacionRepository implements LiquidacionRepositoryPort {

    private final ConcurrentHashMap<String, LiquidacionMensual> almacen = new ConcurrentHashMap<>();

    @Override
    public LiquidacionMensual guardar(LiquidacionMensual liquidacion) {
        almacen.put(liquidacion.id().valor(), liquidacion);
        return liquidacion;
    }

    @Override
    public Optional<LiquidacionMensual> buscarPorId(LiquidacionId id) {
        return Optional.ofNullable(almacen.get(id.valor()));
    }

    @Override
    public List<LiquidacionMensual> buscarPorProsumidor(String prosumidorId) {
        return almacen.values().stream()
                .filter(l -> l.prosumidorId().equals(prosumidorId))
                .collect(Collectors.toList());
    }

    @Override
    public void eliminar(LiquidacionId id) {
        almacen.remove(id.valor());
    }
}
