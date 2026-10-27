package com.voltera.blockchain.domain.port.out;

import com.voltera.blockchain.domain.model.CertificadoNft;

/**
 * Puerto de SALIDA hacia el contrato ERC-721 {@code CertificadoOrigenNFT}. Cada
 * operacion devuelve un resultado con el txHash on-chain (si lo hubo) o la marca
 * de que se ejecuto en modo demo (sin cadena).
 */
public interface CertificadoNftPort {

    /** ¿Hay un contrato NFT desplegado y alcanzable? */
    boolean disponible();

    /** Resultado de una operacion on-chain (o simulada en modo demo). */
    record Resultado(boolean onChain, String txHash, Long bloque, String error) {
        public static Resultado onChain(String txHash, Long bloque) {
            return new Resultado(true, txHash, bloque, null);
        }
        public static Resultado demo(String txHash) {
            return new Resultado(false, txHash, null, null);
        }
        public static Resultado fallo(String error) {
            return new Resultado(false, null, null, error);
        }
        public boolean ok() { return error == null; }
    }

    Resultado emitir(CertificadoNft cert);

    Resultado transferir(long tokenId, String desde, String hacia);

    Resultado quemar(long tokenId, String propietario);
}
