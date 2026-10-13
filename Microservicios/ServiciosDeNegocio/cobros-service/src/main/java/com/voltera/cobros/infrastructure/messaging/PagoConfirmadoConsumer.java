package com.voltera.cobros.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.voltera.cobros.domain.event.PagoConfirmado;
import com.voltera.cobros.domain.model.Movimiento;
import com.voltera.cobros.domain.port.in.ProcesarPagoConfirmadoUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consumidor Kafka del topic {@code topic.cobros.pago-confirmado}. Deserializa el
 * JSON del evento a {@link PagoConfirmado} y lo procesa de forma idempotente. Los
 * errores se registran pero no se relanzan, para no bloquear la partición.
 *
 * <p>{@code @Lazy(false)}: fuerza la creación eager del listener aunque la app use
 * {@code spring.main.lazy-initialization=true}.</p>
 */
@Component
@Lazy(false)
public class PagoConfirmadoConsumer {

    private static final Logger log = LoggerFactory.getLogger(PagoConfirmadoConsumer.class);

    private final ProcesarPagoConfirmadoUseCase procesarUseCase;
    private final ObjectMapper objectMapper;

    public PagoConfirmadoConsumer(ProcesarPagoConfirmadoUseCase procesarUseCase, ObjectMapper objectMapper) {
        this.procesarUseCase = procesarUseCase;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "topic.cobros.pago-confirmado", groupId = "cobros-consumer")
    public void escuchar(String mensaje) {
        try {
            PagoConfirmado evento = objectMapper.readValue(mensaje, PagoConfirmado.class);
            Movimiento mov = procesarUseCase.procesar(evento);
            log.info("Evento PagoConfirmado procesado. eventId={}, referencia={}, movimientoId={}, estado={}",
                    evento.eventId(), evento.referencia(), mov.id().valor(), mov.estado());
        } catch (Exception e) {
            log.error("Error procesando evento PagoConfirmado. payload={}", mensaje, e);
        }
    }
}
