package com.voltera.liquidacion.application;

import com.voltera.liquidacion.domain.model.LiquidacionCalculada;
import com.voltera.liquidacion.domain.model.MensajeOutbox;
import com.voltera.liquidacion.domain.port.out.LiquidacionEventPublisherPort;
import com.voltera.liquidacion.domain.port.out.OutboxPort;
import com.voltera.liquidacion.domain.port.out.SerializadorEventosPort;

/**
 * Registra {@link LiquidacionCalculada} en el OUTBOX (clave = transaccionId).
 * El relay lo publica luego en 'topic.facturacion.liquidacion'; así el servicio
 * no depende de que el broker esté disponible al casar.
 */
public class OutboxLiquidacionEventPublisher implements LiquidacionEventPublisherPort {

    public static final String TIPO_EVENTO = "LiquidacionCalculada";

    private final OutboxPort outbox;
    private final SerializadorEventosPort serializador;

    public OutboxLiquidacionEventPublisher(OutboxPort outbox, SerializadorEventosPort serializador) {
        this.outbox = outbox;
        this.serializador = serializador;
    }

    /** @return {@code true} si el evento quedó registrado en el outbox. */
    @Override
    public boolean publicar(LiquidacionCalculada evento) {
        outbox.agregar(MensajeOutbox.pendiente(TIPO_EVENTO, evento.transaccionId(), serializador.aJson(evento)));
        return true;
    }
}
