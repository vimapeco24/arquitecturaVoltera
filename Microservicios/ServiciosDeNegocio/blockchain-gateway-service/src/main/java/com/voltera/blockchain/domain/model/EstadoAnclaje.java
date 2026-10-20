package com.voltera.blockchain.domain.model;

/**
 * Estados del ciclo de vida de un anclaje (patron OUTBOX).
 *  PENDIENTE   -> registrado localmente, aun no enviado a la cadena.
 *  CONFIRMADO  -> transaccion incluida en un bloque; hash verificado on-chain.
 *  FALLIDO     -> error no transitorio (p. ej. conflicto de hash); requiere revision.
 */
public enum EstadoAnclaje {
    PENDIENTE,
    CONFIRMADO,
    FALLIDO
}
