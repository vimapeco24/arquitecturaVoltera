package com.voltera.pagos.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.voltera.pagos.application.PagoService;
import com.voltera.pagos.infrastructure.config.BeanConfiguration;
import com.voltera.pagos.infrastructure.config.KafkaProducerConfig;
import com.voltera.pagos.infrastructure.messaging.JacksonSerializadorEventos;
import com.voltera.pagos.infrastructure.persistence.InMemoryOutbox;
import com.voltera.pagos.infrastructure.persistence.InMemoryPagoRepository;
import com.voltera.pagos.infrastructure.scheduling.OutboxRelay;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.kafka.core.KafkaTemplate;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Flag pagos.kafka.enabled")
class KafkaHabilitacionTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withBean(ObjectMapper.class, ObjectMapper::new)
            .withUserConfiguration(BeanConfiguration.class, KafkaProducerConfig.class, OutboxRelay.class,
                    InMemoryOutbox.class, InMemoryPagoRepository.class, JacksonSerializadorEventos.class);

    @Test
    @DisplayName("Deshabilitado: arranca sin productor ni relay")
    void deshabilitado() {
        runner.withPropertyValues("pagos.kafka.enabled=false").run(ctx -> {
            assertNull(ctx.getStartupFailure());
            assertEquals(0, ctx.getBeansOfType(OutboxRelay.class).size());
            assertEquals(0, ctx.getBeansOfType(KafkaTemplate.class).size());
            assertEquals(1, ctx.getBeansOfType(PagoService.class).size());
        });
    }

    @Test
    @DisplayName("Habilitado: registra productor y relay")
    void habilitado() {
        runner.withPropertyValues("pagos.kafka.enabled=true").run(ctx -> {
            assertNull(ctx.getStartupFailure());
            assertEquals(1, ctx.getBeansOfType(OutboxRelay.class).size());
            assertEquals(1, ctx.getBeansOfType(KafkaTemplate.class).size());
        });
    }
}
