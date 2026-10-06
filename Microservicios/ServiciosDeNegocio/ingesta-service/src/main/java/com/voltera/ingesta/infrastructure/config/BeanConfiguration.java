package com.voltera.ingesta.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.voltera.ingesta.application.IngestaService;
import com.voltera.ingesta.domain.port.out.OutboxPort;
import com.voltera.ingesta.domain.port.out.SerializadorEventosPort;
import com.voltera.ingesta.domain.port.out.SesionRepositoryPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
public class BeanConfiguration {

    @Bean
    public IngestaService ingestaService(SesionRepositoryPort sesiones,
                                         OutboxPort outbox,
                                         SerializadorEventosPort serializador) {
        return new IngestaService(sesiones, outbox, serializador);
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper().registerModule(new JavaTimeModule());
    }
}
