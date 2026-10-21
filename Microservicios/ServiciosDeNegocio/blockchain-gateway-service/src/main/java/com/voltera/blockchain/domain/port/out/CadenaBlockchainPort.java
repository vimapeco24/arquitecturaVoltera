package com.voltera.blockchain.domain.port.out;

import com.voltera.blockchain.domain.model.Anclaje;

/**
 * Puerto de salida hacia la CADENA. La implementacion (adaptador web3j) habla
 * JSON-RPC con el contrato LiquidacionP2PAnchor en Polygon Amoy a traves del
 * RPC gratuito de un tercero. El dominio no sabe nada de web3j ni de Polygon.
 */
public interface CadenaBlockchainPort {

    /** Resultado de un intento de anclaje on-chain. */
    record ResultadoAnclaje(boolean confirmado, String txHash, Long bloque, Long gasUsado,
                            String error, boolean definitivo) {
        public static ResultadoAnclaje ok(String txHash) {
            return new ResultadoAnclaje(true, txHash, null, null, null, false);
        }
        public static ResultadoAnclaje ok(String txHash, Long bloque, Long gasUsado) {
            return new ResultadoAnclaje(true, txHash, bloque, gasUsado, null, false);
        }
        /** Error transitorio (red, RPC): se reintentara. */
        public static ResultadoAnclaje transitorio(String error) {
            return new ResultadoAnclaje(false, null, null, null, error, false);
        }
        /** Error definitivo (conflicto de hash on-chain, revert no recuperable). */
        public static ResultadoAnclaje definitivo(String error) {
            return new ResultadoAnclaje(false, null, null, null, error, true);
        }
    }

    /** Ancla el hash en la cadena. Idempotente respecto al contrato. */
    ResultadoAnclaje anclar(Anclaje anclaje);

    /** ¿Esta disponible la conexion con la cadena (RPC del tercero)? */
    boolean disponible();
}
