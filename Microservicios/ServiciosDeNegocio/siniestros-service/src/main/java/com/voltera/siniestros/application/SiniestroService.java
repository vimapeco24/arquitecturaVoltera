package com.voltera.siniestros.application;

import com.voltera.siniestros.domain.exception.SiniestroNoEncontradoException;
import com.voltera.siniestros.domain.model.MontoReclamacion;
import com.voltera.siniestros.domain.model.Siniestro;
import com.voltera.siniestros.domain.model.SiniestroAprobado;
import com.voltera.siniestros.domain.model.SiniestroId;
import com.voltera.siniestros.domain.port.in.AprobarSiniestroUseCase;
import com.voltera.siniestros.domain.port.in.ConsultarSiniestroUseCase;
import com.voltera.siniestros.domain.port.in.ReportarSiniestroUseCase;
import com.voltera.siniestros.domain.port.out.EventPublisherPort;
import com.voltera.siniestros.domain.port.out.SiniestroRepositoryPort;

import java.util.List;

/**
 * Servicio de aplicacion: orquesta el ciclo de vida de un siniestro.
 * Al aprobar un siniestro, publica el evento de dominio SiniestroAprobado
 * hacia el broker (EDA) a traves del EventPublisherPort.
 */
public class SiniestroService implements ReportarSiniestroUseCase, AprobarSiniestroUseCase, ConsultarSiniestroUseCase {

    private final SiniestroRepositoryPort repositorio;
    private final EventPublisherPort eventPublisher;

    public SiniestroService(SiniestroRepositoryPort repositorio, EventPublisherPort eventPublisher) {
        this.repositorio = repositorio;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public Siniestro reportar(ComandoReportarSiniestro c) {
        Siniestro siniestro = Siniestro.reportar(
                c.polizaId(),
                c.prosumidorId(),
                c.descripcion(),
                MontoReclamacion.de(c.montoReclamacion()),
                c.fechaOcurrencia());
        return repositorio.guardar(siniestro);
    }

    @Override
    public void eliminar(String siniestroId) {
        Siniestro siniestro = obtener(siniestroId);
        repositorio.eliminar(siniestro.id());
    }

    @Override
    public Siniestro enviarAPeritaje(String siniestroId) {
        Siniestro siniestro = obtener(siniestroId);
        siniestro.enviarAPeritaje();
        return repositorio.guardar(siniestro);
    }

    @Override
    public Siniestro aprobar(String siniestroId) {
        Siniestro siniestro = obtener(siniestroId);
        SiniestroAprobado evento = siniestro.aprobar();
        Siniestro guardado = repositorio.guardar(siniestro);
        eventPublisher.publicar(evento);
        return guardado;
    }

    @Override
    public Siniestro rechazar(String siniestroId) {
        Siniestro siniestro = obtener(siniestroId);
        siniestro.rechazar();
        return repositorio.guardar(siniestro);
    }

    @Override
    public Siniestro porId(String siniestroId) {
        return obtener(siniestroId);
    }

    @Override
    public List<Siniestro> porPoliza(String polizaId) {
        return repositorio.buscarPorPoliza(polizaId);
    }

    @Override
    public List<Siniestro> todos() {
        return repositorio.buscarTodos();
    }

    private Siniestro obtener(String siniestroId) {
        return repositorio.buscarPorId(SiniestroId.de(siniestroId))
                .orElseThrow(() -> new SiniestroNoEncontradoException(siniestroId));
    }
}
