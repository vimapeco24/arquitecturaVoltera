package com.voltera.pagos.infrastructure;

import com.voltera.pagos.domain.model.MensajeOutbox;
import com.voltera.pagos.infrastructure.persistence.InMemoryOutbox;
import com.voltera.pagos.infrastructure.scheduling.OutboxRelay;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;
import java.util.concurrent.CompletableFuture;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@DisplayName("OutboxRelay - publicacion a topic.cobros.pago-confirmado")
class OutboxRelayTest {

    private InMemoryOutbox outbox;
    private KafkaTemplate<String, String> kafka;
    private OutboxRelay relay;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        outbox = new InMemoryOutbox();
        kafka = mock(KafkaTemplate.class);
        relay = new OutboxRelay(outbox, kafka);
    }

    @Test
    @DisplayName("Publica los pendientes al topic correcto y los marca publicados")
    void publica() {
        when(kafka.send(anyString(), anyString(), anyString())).thenReturn(CompletableFuture.completedFuture(null));
        outbox.agregar(MensajeOutbox.pendiente("PagoConfirmado", "FAC-1", "{\"a\":1}"));
        relay.publicarPendientes();
        verify(kafka).send(eq("topic.cobros.pago-confirmado"), eq("FAC-1"), eq("{\"a\":1}"));
        assertEquals(0, outbox.pendientes().size());
    }

    @Test
    @DisplayName("Si el broker falla, el mensaje sigue pendiente y se corta el ciclo")
    void falla() {
        when(kafka.send(anyString(), anyString(), anyString()))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("broker caido")));
        outbox.agregar(MensajeOutbox.pendiente("PagoConfirmado", "FAC-1", "{}"));
        outbox.agregar(MensajeOutbox.pendiente("PagoConfirmado", "FAC-2", "{}"));
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
