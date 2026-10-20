package com.voltera.liquidacion.domain.port.out;

import com.voltera.liquidacion.domain.model.TransaccionP2P;

/**
 * Puerto de SALIDA opcional hacia el servicio de borde blockchain-gateway.
 * Permite que, tras liquidar una transaccion P2P, el server solicite su anclaje
 * en la cadena. La implementacion por defecto es un no-op (cuando el gateway no
 * esta configurado), de modo que el comportamiento actual NO cambia.
 *
 * <p>Es la via "server -> blockchain" cuando el gateway vive FUERA del cluster
 * (PaaS) y Kafka no cruza a la nube: se usa REST a traves de su URL publica.</p>
 */
public interface AnclajeBlockchainPort {
    /** Solicita (best-effort) el anclaje de una transaccion ya liquidada. */
    void solicitarAnclaje(TransaccionP2P transaccion);
}
