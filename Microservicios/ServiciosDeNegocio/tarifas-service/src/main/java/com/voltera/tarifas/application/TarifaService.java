package com.voltera.tarifas.application;

import com.voltera.tarifas.domain.exception.TarifaNoEncontradaException;
import com.voltera.tarifas.domain.model.FranjaHoraria;
import com.voltera.tarifas.domain.model.Precio;
import com.voltera.tarifas.domain.model.Tarifa;
import com.voltera.tarifas.domain.model.TarifaId;
import com.voltera.tarifas.domain.port.in.ConsultarTarifaUseCase;
import com.voltera.tarifas.domain.port.in.GestionarTarifaUseCase;
import com.voltera.tarifas.domain.port.out.TarifaRepositoryPort;

import java.math.BigDecimal;
import java.util.List;

/**
 * Servicio de aplicacion: orquesta la creacion y consulta de tarifas.
 */
public class TarifaService implements GestionarTarifaUseCase, ConsultarTarifaUseCase {

    private final TarifaRepositoryPort repositorio;

    public TarifaService(TarifaRepositoryPort repositorio) {
        this.repositorio = repositorio;
    }

    @Override
    public Tarifa crear(ComandoCrearTarifa c) {
        List<FranjaHoraria> franjas = c.franjas().stream()
                .map(f -> FranjaHoraria.de(f.horaInicio(), f.horaFin(), Precio.porKwh(f.precioKwh())))
                .toList();
        return repositorio.guardar(Tarifa.crear(c.nombre(), franjas));
    }

    @Override
    public Tarifa actualizar(String tarifaId, ComandoCrearTarifa c) {
        TarifaId id = TarifaId.de(tarifaId);
        repositorio.buscarPorId(id)
                .orElseThrow(() -> new TarifaNoEncontradaException(tarifaId));
        List<FranjaHoraria> franjas = c.franjas().stream()
                .map(f -> FranjaHoraria.de(f.horaInicio(), f.horaFin(), Precio.porKwh(f.precioKwh())))
                .toList();
        return repositorio.guardar(Tarifa.crearCon(id, c.nombre(), franjas));
    }

    @Override
    public void eliminar(String tarifaId) {
        TarifaId id = TarifaId.de(tarifaId);
        repositorio.buscarPorId(id)
                .orElseThrow(() -> new TarifaNoEncontradaException(tarifaId));
        repositorio.eliminar(id);
    }

    @Override
    public BigDecimal precioEn(String tarifaId, int hora) {
        return porId(tarifaId).precioEn(hora).porKwh();
    }

    @Override
    public Tarifa porId(String tarifaId) {
        return repositorio.buscarPorId(TarifaId.de(tarifaId))
                .orElseThrow(() -> new TarifaNoEncontradaException(tarifaId));
    }

    @Override
    public List<Tarifa> listar() {
        return repositorio.listar();
    }
}
