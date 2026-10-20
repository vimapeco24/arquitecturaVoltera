package com.voltera.blockchain.infrastructure.crypto;

import com.voltera.blockchain.domain.model.LiquidacionP2P;
import com.voltera.blockchain.domain.port.out.HashServicePort;
import org.web3j.crypto.Hash;
import org.web3j.utils.Numeric;

import java.nio.charset.StandardCharsets;

/**
 * Adaptador de hashing: keccak256 (el mismo que usa Solidity/EVM) sobre la
 * representacion canonica de la liquidacion. Asi el hash que se ancla coincide
 * con el que cualquier auditor puede recomputar.
 */
public class Web3jHashService implements HashServicePort {

    @Override
    public String keccak256Hex(LiquidacionP2P liquidacion) {
        byte[] datos = liquidacion.representacionCanonica().getBytes(StandardCharsets.UTF_8);
        byte[] hash = Hash.sha3(datos); // sha3 de web3j == keccak256
        return Numeric.toHexString(hash); // 0x + 64 hex
    }
}
