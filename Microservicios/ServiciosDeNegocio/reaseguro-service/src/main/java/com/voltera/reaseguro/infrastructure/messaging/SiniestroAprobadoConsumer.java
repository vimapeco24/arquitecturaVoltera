package com.voltera.reaseguro.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.voltera.reaseguro.domain.event.SiniestroAprobado;
import com.voltera.reaseguro.domain.model.CesionRiesgo;
import com.voltera.reaseguro.domain.port.in.ProcesarEventoSiniestroUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consumidor Kafka del topic {@code siniestro-aprobado}. Deserializa el JSON del
 * evento a {@link SiniestroAprobado} y lo procesa de forma idempotente. Los
 * errores se registran pero no se relanzan, para no bloquear la particion.
 *
 * <p>{@code @Lazy(false)}: fuerza la creacion eager aunque la app use
 * {@code spring.main.lazy-initialization=true}; de lo contrario el
 * {@code @KafkaListener} no se registraria al arranque.</p>
 */
@Component
@Lazy(false)
public class SiniestroAprobadoConsumer {

    private static final Logger log = LoggerFactory.getLogger(SiniestroAprobadoConsumer.class);

    private final ProcesarEventoSiniestroUseCase procesarUseCase;
    private final ObjectMapper objectMapper;

    public SiniestroAprobadoConsumer(ProcesarEventoSiniestroUseCase procesarUseCase,
                                     ObjectMapper objectMapper) {
        this.procesarUseCase = procesarUseCase;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "siniestro-aprobado", groupId = "reaseguro-consumer")
    public void escuchar(String mensaje) {
        try {
            SiniestroAprobado evento = objectMapper.readValue(mensaje, SiniestroAprobado.class);
            CesionRiesgo cesion = procesarUseCase.procesar(evento);
            log.info("Evento SiniestroAprobado procesado. eventId={}, siniestroId={}, cesionId={}, estado={}, montoCedido={}",
                    evento.eventId(), evento.siniestroId(), cesion.getId().valor(),
                    cesion.getEstado(), cesion.getMontoCedido());
        } catch (Exception e) {
            log.error("Error procesando evento SiniestroAprobado. payload={}", mensaje, e);
        }
    }
}
