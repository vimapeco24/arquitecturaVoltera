package com.voltera.blockchain.domain.port.in;

import com.voltera.blockchain.domain.model.Anclaje;
import com.voltera.blockchain.domain.model.LiquidacionP2P;

/**
 * Puerto de entrada: recibe una liquidacion P2P del dominio y la registra para
 * ser anclada en la cadena (OUTBOX). Devuelve el anclaje en su estado actual
 * (normalmente PENDIENTE; CONFIRMADO si ya existia y estaba confirmado).
 */
public interface AnclarLiquidacionUseCase {
    Anclaje anclar(LiquidacionP2P liquidacion);
}
