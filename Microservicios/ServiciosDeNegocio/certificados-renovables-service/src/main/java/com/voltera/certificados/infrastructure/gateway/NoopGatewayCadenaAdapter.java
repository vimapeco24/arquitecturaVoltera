package com.voltera.certificados.infrastructure.gateway;

import com.voltera.certificados.domain.model.CertificadoRenovable;
import com.voltera.certificados.domain.port.out.GatewayCadenaPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.SecureRandom;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Adaptador DEMO: se usa cuando no hay gateway configurado. Simula la respuesta
 * del gateway (tokenId incremental + txHash) para que el ciclo de vida del
 * certificado sea completo end-to-end sin cadena ni gateway reales.
 */
public class NoopGatewayCadenaAdapter implements GatewayCadenaPort {

    private static final Logger log = LoggerFactory.getLogger(NoopGatewayCadenaAdapter.class);
    private static final SecureRandom RND = new SecureRandom();
    private final AtomicLong tokenSeq = new AtomicLong(0);

    @Override
    public boolean configurado() {
        return false;
    }

    @Override
    public Resultado emitir(CertificadoRenovable cert) {
        long tokenId = tokenSeq.incrementAndGet();
        log.info("[gateway-demo] emitir NFT certificado={} medidor={} -> tokenId={}",
                cert.id(), cert.idMedidor(), tokenId);
        return Resultado.ok(tokenId, txSimulado());
    }

    @Override
    public Resultado transferir(long tokenId, String desde, String hacia) {
        log.info("[gateway-demo] transferir NFT tokenId={} {} -> {}", tokenId, desde, hacia);
        return Resultado.ok(tokenId, txSimulado());
    }

    @Override
    public Resultado retirar(long tokenId, String titular) {
        log.info("[gateway-demo] retirar NFT tokenId={} de {}", tokenId, titular);
        return Resultado.ok(tokenId, txSimulado());
    }

    private static String txSimulado() {
        byte[] b = new byte[32];
        RND.nextBytes(b);
        StringBuilder sb = new StringBuilder("0x");
        for (byte x : b) sb.append(String.format("%02x", x));
        return sb.toString();
    }
}
