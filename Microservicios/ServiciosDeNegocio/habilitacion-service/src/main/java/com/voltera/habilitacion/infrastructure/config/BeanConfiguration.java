package com.voltera.habilitacion.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.voltera.habilitacion.application.HabilitacionService;
import com.voltera.habilitacion.domain.port.out.MedidorRepositoryPort;
import com.voltera.habilitacion.domain.port.out.OutboxPort;
import com.voltera.habilitacion.domain.port.out.SerializadorEventosPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
public class BeanConfiguration {

    @Bean
    public HabilitacionService habilitacionService(MedidorRepositoryPort repositorio,
                                                   OutboxPort outbox,
                                                   SerializadorEventosPort serializador) {
        return new HabilitacionService(repositorio, outbox, serializador);
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper().registerModule(new JavaTimeModule());
    }
}
