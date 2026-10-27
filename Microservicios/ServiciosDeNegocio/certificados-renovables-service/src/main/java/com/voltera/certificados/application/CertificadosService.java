package com.voltera.certificados.application;

import com.voltera.certificados.domain.exception.CertificadoNoEncontradoException;
import com.voltera.certificados.domain.model.CertificadoRenovable;
import com.voltera.certificados.domain.port.in.GestionarCertificadosUseCase;
import com.voltera.certificados.domain.port.out.CertificadoRepositoryPort;
import com.voltera.certificados.domain.port.out.GatewayCadenaPort;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Servicio de aplicación del BC "Certificados renovables" (lámina 04).
 * Gestiona el ciclo de vida del certificado y SOLICITA al gateway las
 * operaciones on-chain; actualiza el estado local tras la confirmación.
 * No habla web3j (no toca la cadena directamente).
 */
public class CertificadosService implements GestionarCertificadosUseCase {

    private final CertificadoRepositoryPort repo;
    private final GatewayCadenaPort gateway;
    private final AtomicLong secuencia = new AtomicLong(0);

    public CertificadosService(CertificadoRepositoryPort repo, GatewayCadenaPort gateway) {
        this.repo = repo;
        this.gateway = gateway;
    }

    @Override
    public CertificadoRenovable emitir(ComandoEmitir cmd) {
        String id = "CERT-" + secuencia.incrementAndGet();
        CertificadoRenovable cert = new CertificadoRenovable(
                id, cmd.idMedidor(), cmd.emisor(), cmd.titular(),
                cmd.consumoNetoMensualWh(), cmd.periodo(), cmd.solicitudCertificacionId());

        // Solicita la emisión al gateway (anticorruption layer traduce el modelo).
        GatewayCadenaPort.Resultado r = gateway.emitir(cert);
        if (r.ok()) {
            cert.confirmarEmision(r.tokenId() == null ? 0L : r.tokenId(), r.txHash());
        } else {
            cert.marcarFallido(r.error());
        }
        return repo.guardar(cert);
    }

    @Override
    public CertificadoRenovable transferir(String id, String hacia) {
        CertificadoRenovable cert = requerir(id);
        long tokenId = cert.tokenId() == null ? 0L : cert.tokenId();
        GatewayCadenaPort.Resultado r = gateway.transferir(tokenId, cert.titular(), hacia);
        if (!r.ok()) {
            throw new IllegalStateException("El gateway no pudo transferir el NFT: " + r.error());
        }
        cert.transferir(hacia, r.txHash());
        return repo.guardar(cert);
    }

    @Override
    public CertificadoRenovable retirar(String id) {
        CertificadoRenovable cert = requerir(id);
        long tokenId = cert.tokenId() == null ? 0L : cert.tokenId();
        GatewayCadenaPort.Resultado r = gateway.retirar(tokenId, cert.titular());
        if (!r.ok()) {
            throw new IllegalStateException("El gateway no pudo retirar el NFT: " + r.error());
        }
        cert.retirar(r.txHash());
        return repo.guardar(cert);
    }

    @Override
    public CertificadoRenovable porId(String id) {
        return requerir(id);
    }

    @Override
    public List<CertificadoRenovable> todos() {
        return repo.todos();
    }

    @Override
    public List<CertificadoRenovable> porTitular(String titular) {
        return repo.todos().stream()
                .filter(c -> c.titular().equalsIgnoreCase(titular))
                .toList();
    }

    private CertificadoRenovable requerir(String id) {
        return repo.buscar(id).orElseThrow(() -> new CertificadoNoEncontradoException(id));
    }
}
