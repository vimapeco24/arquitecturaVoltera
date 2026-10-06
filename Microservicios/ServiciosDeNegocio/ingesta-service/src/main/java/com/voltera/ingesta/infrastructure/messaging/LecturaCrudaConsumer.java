package com.voltera.ingesta.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.voltera.ingesta.domain.event.LecturaCrudaRecibida;
import com.voltera.ingesta.domain.port.in.IngestarLecturaUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consume el topic {@code ami-lecturas-crudas} (emitido por el ACL de AMI) y lo
 * procesa en la sesion de ingesta (valida/deduplica/emite). La ventana de
 * duplicados del agregado garantiza idempotencia ante reentregas del broker.
 */
@Component
@Lazy(false)
public class LecturaCrudaConsumer {

    private static final Logger log = LoggerFactory.getLogger(LecturaCrudaConsumer.class);

    private final IngestarLecturaUseCase useCase;
    private final ObjectMapper objectMapper;

    public LecturaCrudaConsumer(IngestarLecturaUseCase useCase, ObjectMapper objectMapper) {
        this.useCase = useCase;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "ami-lecturas-crudas", groupId = "ingesta-consumer")
    public void escuchar(String mensaje) {
        try {
            LecturaCrudaRecibida lectura = objectMapper.readValue(mensaje, LecturaCrudaRecibida.class);
            var resultado = useCase.procesar(lectura);
            log.info("LecturaCrudaRecibida procesada. serial={}, resultado={}",
                    lectura.medidorSerial(), resultado);
        } catch (Exception e) {
            log.error("Error procesando LecturaCrudaRecibida. payload={}", mensaje, e);
        }
    }
}
