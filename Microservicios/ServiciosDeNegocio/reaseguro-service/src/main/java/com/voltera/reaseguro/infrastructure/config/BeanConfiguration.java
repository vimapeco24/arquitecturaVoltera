package com.voltera.reaseguro.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.voltera.reaseguro.application.ReaseguroService;
import com.voltera.reaseguro.domain.port.out.CesionRepositoryPort;
import com.voltera.reaseguro.domain.port.out.CesionVistaRepositoryPort;
import com.voltera.reaseguro.domain.port.out.InboxPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wiring de la capa de aplicacion (arquitectura hexagonal).
 */
@Configuration
public class BeanConfiguration {

    @Bean
    public ReaseguroService reaseguroService(CesionRepositoryPort escrituraRepo,
                                             CesionVistaRepositoryPort lecturaRepo,
                                             InboxPort inbox) {
        return new ReaseguroService(escrituraRepo, lecturaRepo, inbox);
    }

    /** ObjectMapper con soporte java.time (Instant) para el consumer Kafka. */
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper().registerModule(new JavaTimeModule());
    }
}
