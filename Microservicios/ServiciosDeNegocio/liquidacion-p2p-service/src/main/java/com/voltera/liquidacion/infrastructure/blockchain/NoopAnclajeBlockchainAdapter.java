package com.voltera.liquidacion.infrastructure.blockchain;

import com.voltera.liquidacion.domain.model.TransaccionP2P;
import com.voltera.liquidacion.domain.port.out.AnclajeBlockchainPort;

/**
 * Implementacion por defecto: no hace nada. Se usa cuando el gateway de
 * blockchain NO esta configurado, de modo que el comportamiento historico del
 * servicio liquidacion-p2p permanece intacto.
 */
public class NoopAnclajeBlockchainAdapter implements AnclajeBlockchainPort {
    @Override
    public void solicitarAnclaje(TransaccionP2P transaccion) {
        // no-op
    }
}
