package com.voltera.telemetria.infrastructure.config;

import com.voltera.telemetria.application.TelemetriaService;
import com.voltera.telemetria.domain.port.out.BufferOfflinePort;
import com.voltera.telemetria.domain.port.out.EventPublisherPort;
import com.voltera.telemetria.domain.port.out.MedidorRepositoryPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wiring de la capa de aplicacion (arquitectura hexagonal).
 */
@Configuration
public class BeanConfiguration {

    @Bean
    public TelemetriaService telemetriaService(MedidorRepositoryPort repositorio,
                                               EventPublisherPort eventPublisher,
                                               BufferOfflinePort buffer) {
        return new TelemetriaService(repositorio, eventPublisher, buffer);
    }
}
