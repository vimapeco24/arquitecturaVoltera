package com.voltera.tarifas.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.voltera.tarifas.domain.port.out.SerializadorEventosPort;
import org.springframework.stereotype.Component;

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
