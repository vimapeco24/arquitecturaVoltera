package com.voltera.blockchain.infrastructure.blockchain;

import com.voltera.blockchain.domain.model.Anclaje;
import com.voltera.blockchain.domain.port.out.CadenaBlockchainPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Adaptador nulo: se usa cuando NO hay RPC/clave/contrato configurados. Permite
 * arrancar el servicio en modo demo (registra en el OUTBOX, deja todo PENDIENTE)
 * sin fallar. Util para desarrollo local o para el frontend antes de desplegar el
 * contrato en Amoy.
 */
public class CadenaNoConfigurada implements CadenaBlockchainPort {

    private static final Logger log = LoggerFactory.getLogger(CadenaNoConfigurada.class);

    @Override
    public ResultadoAnclaje anclar(Anclaje anclaje) {
        log.info("[cadena-no-configurada] Anclaje simulado (queda PENDIENTE) transaccion={} hash={}. "
                + "Configura VOLTERA_BLOCKCHAIN_* para anclar en Polygon Amoy.",
                anclaje.transaccionId(), anclaje.hashCasacion());
        return ResultadoAnclaje.transitorio("cadena no configurada");
    }

    @Override
    public boolean disponible() {
        return false;
    }
}
