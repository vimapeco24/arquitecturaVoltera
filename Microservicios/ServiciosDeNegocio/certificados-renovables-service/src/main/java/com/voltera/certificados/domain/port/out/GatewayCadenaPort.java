package com.voltera.certificados.domain.port.out;

import com.voltera.certificados.domain.model.CertificadoRenovable;

/**
 * Puerto de SALIDA hacia el <b>blockchain-gateway</b> (el ÚNICO que toca la
 * cadena, lámina 04). Este servicio NO habla web3j: le PIDE al gateway que emita/
 * transfiera/retire el NFT y recibe la confirmación. La traducción del modelo de
 * dominio al contrato (anticorruption layer) vive en el adaptador.
 */
public interface GatewayCadenaPort {

    /** ¿Hay un gateway configurado (si no, se trabaja en modo demo)? */
    boolean configurado();

    /** Resultado de pedir una operación al gateway. */
    record Resultado(boolean ok, Long tokenId, String txHash, String error) {
        public static Resultado ok(Long tokenId, String txHash) {
            return new Resultado(true, tokenId, txHash, null);
        }
        public static Resultado fallo(String error) {
            return new Resultado(false, null, null, error);
        }
    }

    /** Pide al gateway acuñar el NFT del certificado. Devuelve tokenId + txHash. */
    Resultado emitir(CertificadoRenovable cert);

    /** Pide al gateway transferir el NFT a otra billetera. */
    Resultado transferir(long tokenId, String desde, String hacia);

    /** Pide al gateway quemar/retirar el NFT. */
    Resultado retirar(long tokenId, String titular);
}
