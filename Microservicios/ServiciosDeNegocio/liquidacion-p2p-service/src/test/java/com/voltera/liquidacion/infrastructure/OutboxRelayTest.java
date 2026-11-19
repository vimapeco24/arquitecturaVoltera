package com.voltera.liquidacion.infrastructure;

import com.voltera.liquidacion.domain.model.MensajeOutbox;
import com.voltera.liquidacion.infrastructure.persistence.InMemoryOutbox;
import com.voltera.liquidacion.infrastructure.scheduling.OutboxRelay;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@DisplayName("OutboxRelay - publicacion a topic.facturacion.liquidacion")
class OutboxRelayTest {

    private InMemoryOutbox outbox;
    private KafkaTemplate<String, String> kafka;
    private OutboxRelay relay;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        outbox = new InMemoryOutbox();
        kafka = mock(KafkaTemplate.class);
        relay = new OutboxRelay(outbox, kafka, "topic.facturacion.liquidacion");
    }

    @Test
    @DisplayName("Publica los pendientes al topic y los marca publicados")
    void publica() {
        when(kafka.send(anyString(), anyString(), anyString())).thenReturn(CompletableFuture.completedFuture(null));
        outbox.agregar(MensajeOutbox.pendiente("LiquidacionCalculada", "TX-1", "{\"a\":1}"));

        relay.publicarPendientes();

        verify(kafka).send(eq("topic.facturacion.liquidacion"), eq("TX-1"), eq("{\"a\":1}"));
        assertEquals(0, outbox.pendientes().size());
    }

    @Test
    @DisplayName("Si el broker falla, el mensaje sigue pendiente y se corta el ciclo")
    void falla() {
        when(kafka.send(anyString(), anyString(), anyString()))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("broker caido")));
        outbox.agregar(MensajeOutbox.pendiente("LiquidacionCalculada", "TX-1", "{}"));
        outbox.agregar(MensajeOutbox.pendiente("LiquidacionCalculada", "TX-2", "{}"));

        relay.publicarPendientes();

        verify(kafka, times(1)).send(anyString(), anyString(), anyString());
        assertEquals(2, outbox.pendientes().size());
    }

    @Test
    @DisplayName("Sin pendientes no envia nada")
    void vacio() {
        relay.publicarPendientes();
        verifyNoInteractions(kafka);
    }
}
