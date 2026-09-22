package com.voltera.liquidacion.domain.port.in;

import com.voltera.liquidacion.domain.model.TransaccionP2P;

import java.util.List;

/**
 * Puerto de ENTRADA (driving port): consultas de transacciones P2P.
 */
public interface ConsultarTransaccionUseCase {

    TransaccionP2P porId(String transaccionId);

    List<TransaccionP2P> porProsumidor(String prosumidorId);
}
