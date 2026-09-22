package com.voltera.tarifaeventos.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.voltera.tarifaeventos.application.TarifaEventosService;
import com.voltera.tarifaeventos.domain.port.out.CargoRepositoryPort;
import com.voltera.tarifaeventos.domain.port.out.CargoVistaRepositoryPort;
import com.voltera.tarifaeventos.domain.port.out.NotificacionPublisherPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wiring de la capa de aplicacion (arquitectura hexagonal).
 */
@Configuration
public class BeanConfiguration {

    @Bean
    public TarifaEventosService tarifaEventosService(CargoRepositoryPort escrituraRepo,
                                                     CargoVistaRepositoryPort lecturaRepo,
                                                     NotificacionPublisherPort notificacionPublisher) {
        return new TarifaEventosService(escrituraRepo, lecturaRepo, notificacionPublisher);
    }

    /** ObjectMapper con soporte para java.time (Instant) usado por consumer y publisher. */
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper().registerModule(new JavaTimeModule());
    }
}
