package com.voltera.liquidacion.infrastructure.messaging;

import com.voltera.liquidacion.domain.model.LiquidacionCalculada;
import com.voltera.liquidacion.domain.port.out.LiquidacionEventPublisherPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Adaptador de SALIDA por defecto: NO publica a Kafka. Se usa cuando
 * {@code voltera.kafka.enabled} es false (p. ej. en Render, donde el broker es
 * LAN). Así el servicio funciona igual que siempre (anclaje solo por REST)
 * mientras no se habilite el event mesh.
 */
public class NoopLiquidacionEventPublisher implements LiquidacionEventPublisherPort {

    private static final Logger log = LoggerFactory.getLogger(NoopLiquidacionEventPublisher.class);

    @Override
    public boolean publicar(LiquidacionCalculada evento) {
        log.debug("EDA deshabilitado (voltera.kafka.enabled=false): no se publica LiquidacionCalculada transaccionId={}",
                evento.transaccionId());
        return false;
    }
}
