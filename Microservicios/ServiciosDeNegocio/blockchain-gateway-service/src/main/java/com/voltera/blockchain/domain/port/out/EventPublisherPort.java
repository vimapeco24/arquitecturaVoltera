package com.voltera.blockchain.domain.port.out;

import com.voltera.blockchain.domain.model.LiquidacionAnclada;

/**
 * Puerto de salida (EDA): publica el evento de dominio LiquidacionAnclada en el
 * broker. Devuelve true si el broker confirmo; false para que el llamador reintente.
 */
public interface EventPublisherPort {
    boolean publicar(LiquidacionAnclada evento);
}
