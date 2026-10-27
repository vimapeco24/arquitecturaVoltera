package com.voltera.blockchain.infrastructure.blockchain;

import com.voltera.blockchain.domain.model.CertificadoNft;
import com.voltera.blockchain.domain.port.out.CertificadoNftPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.SecureRandom;

/**
 * Adaptador NFT en modo DEMO: se usa cuando el contrato {@code CertificadoOrigenNFT}
 * no esta desplegado/alcanzable. Simula el txHash para que el flujo
 * emision/transferencia/quema sea completo end-to-end sin cadena real. El estado
 * (propiedad, historial) lo mantiene el servicio en memoria.
 */
public class CertificadoNftDemoAdapter implements CertificadoNftPort {

    private static final Logger log = LoggerFactory.getLogger(CertificadoNftDemoAdapter.class);
    private static final SecureRandom RND = new SecureRandom();

    @Override
    public boolean disponible() {
        return false;
    }

    @Override
    public Resultado emitir(CertificadoNft cert) {
        log.info("[nft-demo] emitir token={} medidor={} periodo={} -> {}",
                cert.tokenId(), cert.idMedidor(), cert.periodo(), cert.propietario());
        return Resultado.demo(txSimulado());
    }

    @Override
    public Resultado transferir(long tokenId, String desde, String hacia) {
        log.info("[nft-demo] transferir token={} {} -> {}", tokenId, desde, hacia);
        return Resultado.demo(txSimulado());
    }

    @Override
    public Resultado quemar(long tokenId, String propietario) {
        log.info("[nft-demo] quemar token={} de {}", tokenId, propietario);
        return Resultado.demo(txSimulado());
    }

    private static String txSimulado() {
        byte[] b = new byte[32];
        RND.nextBytes(b);
        StringBuilder sb = new StringBuilder("0x");
        for (byte x : b) sb.append(String.format("%02x", x));
        return sb.toString();
    }
}
