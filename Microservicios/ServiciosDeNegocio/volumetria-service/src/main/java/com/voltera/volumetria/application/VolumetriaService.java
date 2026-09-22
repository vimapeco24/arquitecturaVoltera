package com.voltera.volumetria.application;

import com.voltera.volumetria.domain.exception.LecturaNoEncontradaException;
import com.voltera.volumetria.domain.model.LecturaId;
import com.voltera.volumetria.domain.model.LecturaTelemetria;
import com.voltera.volumetria.domain.model.Medida;
import com.voltera.volumetria.domain.model.MedidorId;
import com.voltera.volumetria.domain.port.in.ConsultarLecturaUseCase;
import com.voltera.volumetria.domain.port.in.IngestarLecturaUseCase;
import com.voltera.volumetria.domain.port.out.LecturaRepositoryPort;

import java.util.List;

/**
 * Servicio de aplicacion: orquesta la ingesta y consulta de lecturas.
 * Agnostico de framework.
 */
public class VolumetriaService implements IngestarLecturaUseCase, ConsultarLecturaUseCase {

    private final LecturaRepositoryPort repositorio;

    public VolumetriaService(LecturaRepositoryPort repositorio) {
        this.repositorio = repositorio;
    }

    @Override
    public LecturaTelemetria ingestar(ComandoIngestarLectura c) {
        Medida.Direccion direccion = Medida.Direccion.valueOf(c.direccion().toUpperCase());
        Medida medida = Medida.de(c.kwh(), direccion);
        LecturaTelemetria lectura = LecturaTelemetria.ingestar(
                MedidorId.de(c.medidorId()), medida, c.capturadaEn());
        return repositorio.guardar(lectura);
    }

    @Override
    public LecturaTelemetria actualizar(String lecturaId, ComandoIngestarLectura c) {
        LecturaId id = LecturaId.de(lecturaId);
        repositorio.buscarPorId(id)
                .orElseThrow(() -> new LecturaNoEncontradaException(lecturaId));
        Medida.Direccion direccion = Medida.Direccion.valueOf(c.direccion().toUpperCase());
        Medida medida = Medida.de(c.kwh(), direccion);
        LecturaTelemetria lectura = LecturaTelemetria.ingestarCon(
                id, MedidorId.de(c.medidorId()), medida, c.capturadaEn());
        return repositorio.guardar(lectura);
    }

    @Override
    public void eliminar(String lecturaId) {
        LecturaId id = LecturaId.de(lecturaId);
        repositorio.buscarPorId(id)
                .orElseThrow(() -> new LecturaNoEncontradaException(lecturaId));
        repositorio.eliminar(id);
    }

    @Override
    public LecturaTelemetria porId(String lecturaId) {
        return repositorio.buscarPorId(LecturaId.de(lecturaId))
                .orElseThrow(() -> new LecturaNoEncontradaException(lecturaId));
    }

    @Override
    public List<LecturaTelemetria> porMedidor(String medidorId) {
        return repositorio.buscarPorMedidor(medidorId);
    }
}
