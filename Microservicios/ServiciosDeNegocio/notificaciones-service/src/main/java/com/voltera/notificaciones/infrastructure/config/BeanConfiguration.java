package com.voltera.notificaciones.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.voltera.notificaciones.application.NotificacionService;
import com.voltera.notificaciones.domain.port.out.NotificacionRepositoryPort;
import com.voltera.notificaciones.domain.port.out.OutboxPort;
import com.voltera.notificaciones.domain.port.out.PreferenciaRepositoryPort;
import com.voltera.notificaciones.domain.port.out.SerializadorEventosPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
public class BeanConfiguration {

    @Bean
    public NotificacionService notificacionService(
            NotificacionRepositoryPort repositorio,
            PreferenciaRepositoryPort preferencias,
            OutboxPort outbox,
            SerializadorEventosPort serializador) {
        return new NotificacionService(repositorio, preferencias, outbox, serializador);
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper().registerModule(new JavaTimeModule());
    }
}
