package com.voltera.ingesta.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.voltera.ingesta.domain.event.LecturaCrudaRecibida;
import com.voltera.ingesta.domain.port.in.IngestarLecturaUseCase;
import com.voltera.ingesta.domain.port.out.InboxPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consume el topic {@code ami-lecturas-crudas} (emitido por el ACL de AMI) y lo
 * procesa en la sesion de ingesta (valida/deduplica/emite). Idempotente en dos
 * capas (lamina 10): tabla inbox por {@code eventId} ante reentregas del broker, y
 * ventana de duplicados del agregado ante relecturas de negocio.
 */
@Component
@Lazy(false)
public class LecturaCrudaConsumer {

    private static final Logger log = LoggerFactory.getLogger(LecturaCrudaConsumer.class);
    private static final String CONSUMIDOR = "ingesta-consumer#ami-lecturas-crudas";

    private final IngestarLecturaUseCase useCase;
    private final InboxPort inbox;
    private final ObjectMapper objectMapper;

    public LecturaCrudaConsumer(IngestarLecturaUseCase useCase, InboxPort inbox,
                                ObjectMapper objectMapper) {
        this.useCase = useCase;
        this.inbox = inbox;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "ami-lecturas-crudas", groupId = "ingesta-consumer")
    public void escuchar(String mensaje) {
        try {
            LecturaCrudaRecibida lectura = objectMapper.readValue(mensaje, LecturaCrudaRecibida.class);
            String clave = (lectura.eventId() != null)
                    ? lectura.eventId()
                    : "LecturaCrudaRecibida|" + lectura.medidorSerial() + "|" + lectura.capturadaEn();
            if (!inbox.registrarSiEsNuevo(CONSUMIDOR, clave, "LecturaCrudaRecibida")) {
                log.info("LecturaCrudaRecibida duplicada ignorada. eventId={}", lectura.eventId());
                return;
            }
            var resultado = useCase.procesar(lectura);
            log.info("LecturaCrudaRecibida procesada. serial={}, resultado={}",
                    lectura.medidorSerial(), resultado);
        } catch (Exception e) {
            log.error("Error procesando LecturaCrudaRecibida. payload={}", mensaje, e);
        }
    }
}
