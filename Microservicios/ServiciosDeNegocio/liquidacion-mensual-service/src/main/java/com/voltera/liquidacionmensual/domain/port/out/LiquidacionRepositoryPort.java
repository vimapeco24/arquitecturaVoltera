package com.voltera.liquidacionmensual.domain.port.out;

import com.voltera.liquidacionmensual.domain.model.LiquidacionId;
import com.voltera.liquidacionmensual.domain.model.LiquidacionMensual;

import java.util.List;
import java.util.Optional;

/**
 * Puerto de SALIDA: persistencia de liquidaciones mensuales.
 */
public interface LiquidacionRepositoryPort {

    LiquidacionMensual guardar(LiquidacionMensual liquidacion);

    Optional<LiquidacionMensual> buscarPorId(LiquidacionId id);

    List<LiquidacionMensual> buscarPorProsumidor(String prosumidorId);

    void eliminar(LiquidacionId id);
}
