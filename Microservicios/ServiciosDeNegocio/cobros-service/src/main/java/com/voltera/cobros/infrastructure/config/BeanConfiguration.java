package com.voltera.cobros.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.voltera.cobros.application.CobrosService;
import com.voltera.cobros.domain.port.out.InboxPort;
import com.voltera.cobros.domain.port.out.MovimientoRepositoryPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wiring de la capa de aplicación (arquitectura hexagonal).
 */
@Configuration
public class BeanConfiguration {

    @Bean
    public CobrosService cobrosService(MovimientoRepositoryPort repositorio, InboxPort inbox) {
        return new CobrosService(repositorio, inbox);
    }

    /** ObjectMapper con soporte java.time (Instant) para el consumer Kafka. */
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper().registerModule(new JavaTimeModule());
    }
}
