package com.voltera.liquidacion.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.voltera.liquidacion.domain.port.out.SerializadorEventosPort;

/** Serializador JSON de eventos (Jackson). */
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
