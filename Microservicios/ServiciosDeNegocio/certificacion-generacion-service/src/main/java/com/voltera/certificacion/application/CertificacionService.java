package com.voltera.certificacion.application;

import com.voltera.certificacion.domain.exception.SolicitudNoEncontradaException;
import com.voltera.certificacion.domain.model.BalanceGeneracionCertificable;
import com.voltera.certificacion.domain.model.SolicitudCertificacion;
import com.voltera.certificacion.domain.port.in.CertificarGeneracionUseCase;
import com.voltera.certificacion.domain.port.out.SolicitudRepositoryPort;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Servicio de aplicación del BC "Certificación de generación" (lámina 04).
 * Orquesta el balance certificable y el ciclo de vida de las solicitudes.
 * <b>NO toca la cadena</b> — es dominio puro off-chain; otro servicio
 * (Certificados Renovables) usará el gateway para emitir el NFT.
 */
public class CertificacionService implements CertificarGeneracionUseCase {

    private final SolicitudRepositoryPort repo;
    private final long bloqueWh;
    private final AtomicLong secuencia = new AtomicLong(0);

    public CertificacionService(SolicitudRepositoryPort repo, long bloqueWh) {
        this.repo = repo;
        this.bloqueWh = bloqueWh;
    }

    @Override
    public BalanceGeneracionCertificable registrarGeneracion(ComandoRegistrarGeneracion cmd) {
        BalanceGeneracionCertificable balance = repo.buscarBalance(cmd.medidorId())
                .orElseGet(() -> new BalanceGeneracionCertificable(cmd.medidorId(), bloqueWh));
        balance.registrarGeneracion(cmd.medicionId(), cmd.wh());
        return repo.guardarBalance(balance);
    }

    @Override
    public SolicitudCertificacion solicitarCertificacion(ComandoSolicitar cmd) {
        BalanceGeneracionCertificable balance = repo.buscarBalance(cmd.medidorId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "No hay balance de generación para el medidor " + cmd.medidorId()));
        // Reserva un bloque (consume energía del acumulado) — invariante de no reutilización.
        balance.reservarBloque();
        repo.guardarBalance(balance);

        String id = "SOL-" + secuencia.incrementAndGet();
        SolicitudCertificacion solicitud = new SolicitudCertificacion(
                id, cmd.medidorId(), cmd.propietario(), balance.bloqueWh(), cmd.periodo());
        return repo.guardarSolicitud(solicitud);
    }

    @Override
    public SolicitudCertificacion avalarVoltera(String solicitudId) {
        SolicitudCertificacion s = requerir(solicitudId);
        s.avalarVoltera();
        return repo.guardarSolicitud(s);
    }

    @Override
    public SolicitudCertificacion avalarCertificadora(String solicitudId) {
        SolicitudCertificacion s = requerir(solicitudId);
        s.avalarCertificadora();
        return repo.guardarSolicitud(s);
    }

    @Override
    public SolicitudCertificacion rechazar(String solicitudId, String motivo) {
        SolicitudCertificacion s = requerir(solicitudId);
        s.rechazar(motivo);
        return repo.guardarSolicitud(s);
    }

    @Override
    public SolicitudCertificacion porId(String solicitudId) {
        return requerir(solicitudId);
    }

    @Override
    public List<SolicitudCertificacion> todas() {
        return repo.todasLasSolicitudes();
    }

    @Override
    public BalanceGeneracionCertificable balanceDe(String medidorId) {
        return repo.buscarBalance(medidorId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No hay balance de generación para el medidor " + medidorId));
    }

    @Override
    public List<BalanceGeneracionCertificable> balances() {
        return repo.todosLosBalances();
    }

    private SolicitudCertificacion requerir(String id) {
        return repo.buscarSolicitud(id).orElseThrow(() -> new SolicitudNoEncontradaException(id));
    }
}
