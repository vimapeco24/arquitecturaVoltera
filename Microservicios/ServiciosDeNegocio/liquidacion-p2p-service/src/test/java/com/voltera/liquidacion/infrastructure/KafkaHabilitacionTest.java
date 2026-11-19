package com.voltera.liquidacion.infrastructure;

import com.voltera.liquidacion.domain.port.out.LiquidacionEventPublisherPort;
import com.voltera.liquidacion.application.OutboxLiquidacionEventPublisher;
import com.voltera.liquidacion.infrastructure.config.MessagingConfiguration;
import com.voltera.liquidacion.infrastructure.messaging.NoopLiquidacionEventPublisher;
import com.voltera.liquidacion.infrastructure.persistence.InMemoryOutbox;
import com.voltera.liquidacion.infrastructure.scheduling.OutboxRelay;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.kafka.core.KafkaTemplate;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Flag voltera.kafka.enabled")
class KafkaHabilitacionTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(MessagingConfiguration.class, InMemoryOutbox.class, OutboxRelay.class);

    @Test
    @DisplayName("Deshabilitado (defecto): arranca con publisher no-op, sin outbox, relay ni productor")
    void deshabilitado() {
        runner.run(ctx -> {
            assertNull(ctx.getStartupFailure());
            assertEquals(0, ctx.getBeansOfType(OutboxRelay.class).size());
            assertEquals(0, ctx.getBeansOfType(InMemoryOutbox.class).size());
            assertEquals(0, ctx.getBeansOfType(KafkaTemplate.class).size());
            assertInstanceOf(NoopLiquidacionEventPublisher.class, ctx.getBean(LiquidacionEventPublisherPort.class));
        });
    }

    @Test
    @DisplayName("Habilitado: publisher por outbox, relay y productor")
    void habilitado() {
        runner.withPropertyValues("voltera.kafka.enabled=true").run(ctx -> {
            assertNull(ctx.getStartupFailure());
            assertEquals(1, ctx.getBeansOfType(OutboxRelay.class).size());
            assertEquals(1, ctx.getBeansOfType(InMemoryOutbox.class).size());
            assertEquals(1, ctx.getBeansOfType(KafkaTemplate.class).size());
            assertInstanceOf(OutboxLiquidacionEventPublisher.class, ctx.getBean(LiquidacionEventPublisherPort.class));
        });
    }
}
