package com.voltera.liquidacion.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.voltera.liquidacion.infrastructure.messaging.JacksonSerializadorEventos;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("JacksonSerializadorEventos")
class JacksonSerializadorEventosTest {

    private final JacksonSerializadorEventos serializador = new JacksonSerializadorEventos(new ObjectMapper());

    @Test
    @DisplayName("Serializa un mapa a JSON")
    void serializa() {
        assertEquals("{\"a\":1}", serializador.aJson(Map.of("a", 1)));
    }

    @Test
    @DisplayName("Un payload no serializable lanza IllegalStateException")
    void falla() {
        assertThrows(IllegalStateException.class, () -> serializador.aJson(new Object()));
    }
}
