package com.voltera.tarifaeventos.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.voltera.tarifaeventos.domain.event.ConsumoRegistrado;
import com.voltera.tarifaeventos.domain.model.CargoTarifa;
import com.voltera.tarifaeventos.domain.port.in.ProcesarConsumoUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consumidor Kafka del topic {@code consumo-registrado} (emitido por
 * telemetria-service). Deserializa el JSON del evento a {@link ConsumoRegistrado}
 * y lo procesa de forma idempotente. Los errores se registran pero no se
 * relanzan, para no bloquear la particion.
 *
 * <p>{@code @Lazy(false)}: fuerza la creacion eager de este bean aunque la app
 * use {@code spring.main.lazy-initialization=true}; de lo contrario el
 * {@code @KafkaListener} no se registraria al arranque y no se consumiria nada.</p>
 */
@Component
@Lazy(false)
public class ConsumoRegistradoConsumer {

    private static final Logger log = LoggerFactory.getLogger(ConsumoRegistradoConsumer.class);

    private final ProcesarConsumoUseCase procesarUseCase;
    private final ObjectMapper objectMapper;

    public ConsumoRegistradoConsumer(ProcesarConsumoUseCase procesarUseCase,
                                     ObjectMapper objectMapper) {
        this.procesarUseCase = procesarUseCase;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "consumo-registrado", groupId = "tarifa-eventos-consumer")
    public void escuchar(String mensaje) {
        try {
            ConsumoRegistrado evento = objectMapper.readValue(mensaje, ConsumoRegistrado.class);
            CargoTarifa cargo = procesarUseCase.procesar(evento);
            log.info("Evento ConsumoRegistrado procesado. eventId={}, medidorId={}, cargoId={}, estado={}, monto={}",
                    evento.eventId(), evento.medidorId(), cargo.getId().valor(),
                    cargo.getEstado(), cargo.getMontoCargo());
        } catch (Exception e) {
            log.error("Error procesando evento ConsumoRegistrado. payload={}", mensaje, e);
        }
    }
}
