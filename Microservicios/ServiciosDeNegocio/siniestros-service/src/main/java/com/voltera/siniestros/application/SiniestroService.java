package com.voltera.siniestros.application;

import com.voltera.siniestros.domain.exception.SiniestroNoEncontradoException;
import com.voltera.siniestros.domain.model.MontoReclamacion;
import com.voltera.siniestros.domain.model.Siniestro;
import com.voltera.siniestros.domain.model.SiniestroAprobado;
import com.voltera.siniestros.domain.model.SiniestroId;
import com.voltera.siniestros.domain.port.in.AprobarSiniestroUseCase;
import com.voltera.siniestros.domain.port.in.ConsultarSiniestroUseCase;
import com.voltera.siniestros.domain.port.in.DrenarBufferUseCase;
import com.voltera.siniestros.domain.port.in.ReportarSiniestroUseCase;
import com.voltera.siniestros.domain.port.out.BufferOfflinePort;
import com.voltera.siniestros.domain.port.out.EventPublisherPort;
import com.voltera.siniestros.domain.port.out.SiniestroRepositoryPort;

import java.util.List;

/**
 * Servicio de aplicacion: orquesta el ciclo de vida de un siniestro.
 *
 * <p>Al aprobar un siniestro, publica el evento de dominio SiniestroAprobado
 * hacia el broker (EDA). La publicacion sigue el patron <b>store-and-forward</b>:
 * si el broker no confirma la entrega, el evento se encola en el buffer offline
 * y un job programado lo reenvia al recuperar la conexion, de modo que ninguna
 * aprobacion se pierde por una caida del broker.</p>
 */
public class SiniestroService implements ReportarSiniestroUseCase, AprobarSiniestroUseCase,
        ConsultarSiniestroUseCase, DrenarBufferUseCase {

    private final SiniestroRepositoryPort repositorio;
    private final EventPublisherPort eventPublisher;
    private final BufferOfflinePort buffer;

    public SiniestroService(SiniestroRepositoryPort repositorio,
                            EventPublisherPort eventPublisher,
                            BufferOfflinePort buffer) {
        this.repositorio = repositorio;
        this.eventPublisher = eventPublisher;
        this.buffer = buffer;
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
        // Store-and-forward: si el broker no confirma, el evento se encola para reenvio.
        boolean publicado = eventPublisher.publicar(evento);
        if (!publicado) {
            buffer.encolar(evento);
        }
        return guardado;
    }

    // ---------------------------------------------------------------------
    // Desencolado del buffer offline (store-and-forward -> forward)
    // ---------------------------------------------------------------------

    @Override
    public ResultadoDrenado drenar() {
        int reenviados = 0;
        for (SiniestroAprobado evento : buffer.pendientes()) {
            boolean ok = eventPublisher.publicar(evento);
            if (ok) {
                buffer.confirmarEntrega(evento);
                reenviados++;
            } else {
                // El broker sigue caido: paramos y reintentaremos en el proximo ciclo.
                break;
            }
        }
        return new ResultadoDrenado(reenviados, buffer.tamano());
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
