package com.voltera.blockchain.domain.port.out;

import com.voltera.blockchain.domain.model.LiquidacionP2P;

/**
 * Puerto de salida: calcula el hash keccak256 del registro canonico (lo que se
 * ancla on-chain). Se aisla como puerto porque keccak256 lo provee web3j/crypto.
 */
public interface HashServicePort {
    /** @return hash keccak256 en hex con prefijo 0x (32 bytes). */
    String keccak256Hex(LiquidacionP2P liquidacion);
}
