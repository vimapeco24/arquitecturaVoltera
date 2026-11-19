package com.voltera.blockchain.domain.port.in;

import com.voltera.blockchain.domain.model.LecturaGobernanza;

/** Caso de uso: leer el estado on-chain de una propuesta multi-firma. */
public interface ConsultarGobernanzaUseCase {
    LecturaGobernanza leer(String propuestaId);
}
