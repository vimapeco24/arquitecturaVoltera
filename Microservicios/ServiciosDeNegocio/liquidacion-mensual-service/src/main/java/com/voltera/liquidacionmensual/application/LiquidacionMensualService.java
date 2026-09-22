package com.voltera.liquidacionmensual.application;

import com.voltera.liquidacionmensual.domain.exception.LiquidacionNoEncontradaException;
import com.voltera.liquidacionmensual.domain.model.Energia;
import com.voltera.liquidacionmensual.domain.model.LiquidacionId;
import com.voltera.liquidacionmensual.domain.model.LiquidacionMensual;
import com.voltera.liquidacionmensual.domain.model.Movimiento;
import com.voltera.liquidacionmensual.domain.model.Periodo;
import com.voltera.liquidacionmensual.domain.port.in.ConsultarLiquidacionUseCase;
import com.voltera.liquidacionmensual.domain.port.in.GestionarLiquidacionUseCase;
import com.voltera.liquidacionmensual.domain.port.out.LiquidacionRepositoryPort;

import java.time.Instant;
import java.util.List;

/**
 * Servicio de aplicacion: orquesta la apertura, acumulacion y cierre de
 * liquidaciones mensuales. Las reglas de totalizacion viven en el dominio.
 */
public class LiquidacionMensualService implements GestionarLiquidacionUseCase, ConsultarLiquidacionUseCase {

    private final LiquidacionRepositoryPort repositorio;

    public LiquidacionMensualService(LiquidacionRepositoryPort repositorio) {
        this.repositorio = repositorio;
    }

    @Override
    public LiquidacionMensual abrir(ComandoAbrir c) {
        LiquidacionMensual liquidacion = LiquidacionMensual.abrir(
                c.prosumidorId(), Periodo.de(c.anio(), c.mes()),
                c.precioConsumoKwh(), c.precioExcedenteKwh());
        return repositorio.guardar(liquidacion);
    }

    @Override
    public LiquidacionMensual actualizar(String liquidacionId, ComandoAbrir c) {
        LiquidacionId id = LiquidacionId.de(liquidacionId);
        repositorio.buscarPorId(id)
                .orElseThrow(() -> new LiquidacionNoEncontradaException(liquidacionId));
        LiquidacionMensual liquidacion = LiquidacionMensual.abrirCon(
                id, c.prosumidorId(), Periodo.de(c.anio(), c.mes()),
                c.precioConsumoKwh(), c.precioExcedenteKwh());
        return repositorio.guardar(liquidacion);
    }

    @Override
    public void eliminar(String liquidacionId) {
        LiquidacionId id = LiquidacionId.de(liquidacionId);
        repositorio.buscarPorId(id)
                .orElseThrow(() -> new LiquidacionNoEncontradaException(liquidacionId));
        repositorio.eliminar(id);
    }

    @Override
    public LiquidacionMensual registrarMovimiento(ComandoMovimiento c) {
        LiquidacionMensual liquidacion = porId(c.liquidacionId());
        Movimiento.Tipo tipo = Movimiento.Tipo.valueOf(c.tipo().toUpperCase());
        Movimiento movimiento = Movimiento.de(tipo, Energia.deKwh(c.kwh()), Instant.now());
        liquidacion.registrarMovimiento(movimiento);
        return repositorio.guardar(liquidacion);
    }

    @Override
    public LiquidacionMensual cerrar(String liquidacionId) {
        LiquidacionMensual liquidacion = porId(liquidacionId);
        liquidacion.cerrar();
        return repositorio.guardar(liquidacion);
    }

    @Override
    public LiquidacionMensual porId(String liquidacionId) {
        return repositorio.buscarPorId(LiquidacionId.de(liquidacionId))
                .orElseThrow(() -> new LiquidacionNoEncontradaException(liquidacionId));
    }

    @Override
    public List<LiquidacionMensual> porProsumidor(String prosumidorId) {
        return repositorio.buscarPorProsumidor(prosumidorId);
    }
}
