package com.voltera.siniestros.infrastructure.config;

import com.voltera.siniestros.application.SiniestroService;
import com.voltera.siniestros.domain.port.out.BufferOfflinePort;
import com.voltera.siniestros.domain.port.out.EventPublisherPort;
import com.voltera.siniestros.domain.port.out.SiniestroRepositoryPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BeanConfiguration {

    @Bean
    public SiniestroService siniestroService(SiniestroRepositoryPort repositorio,
                                             EventPublisherPort eventPublisher,
                                             BufferOfflinePort buffer) {
        return new SiniestroService(repositorio, eventPublisher, buffer);
    }
}
