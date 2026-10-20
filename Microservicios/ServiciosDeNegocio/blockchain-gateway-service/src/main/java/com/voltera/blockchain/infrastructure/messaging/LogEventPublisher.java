package com.voltera.blockchain.infrastructure.messaging;

import com.voltera.blockchain.domain.model.LiquidacionAnclada;
import com.voltera.blockchain.domain.port.out.EventPublisherPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Publicador de respaldo: si Kafka esta deshabilitado (demo local), registra el
 * evento por log y lo da por "publicado" para no bloquear el flujo.
 */
public class LogEventPublisher implements EventPublisherPort {

    private static final Logger log = LoggerFactory.getLogger(LogEventPublisher.class);

    @Override
    public boolean publicar(LiquidacionAnclada evento) {
        log.info("[kafka-off] LiquidacionAnclada transaccion={} txHash={} energiaWh={}",
                evento.transaccionId(), evento.txHash(), evento.energiaWh());
        return true;
    }
}
