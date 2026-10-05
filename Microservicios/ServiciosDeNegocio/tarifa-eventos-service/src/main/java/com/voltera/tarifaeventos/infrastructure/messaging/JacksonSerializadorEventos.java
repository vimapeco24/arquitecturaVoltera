package com.voltera.tarifaeventos.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.voltera.tarifaeventos.domain.port.out.SerializadorEventosPort;
import org.springframework.stereotype.Component;

/**
 * Adaptador de SALIDA: serializa payloads de eventos a JSON con Jackson, para
 * almacenarlos en el outbox.
 */
@Component
public class JacksonSerializadorEventos implements SerializadorEventosPort {

    private final ObjectMapper objectMapper;

    public JacksonSerializadorEventos(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public String aJson(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo serializar el payload del evento", e);
        }
    }
}
