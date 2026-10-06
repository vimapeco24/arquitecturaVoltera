package com.voltera.integracionami.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.voltera.integracionami.application.IntegracionAmiService;
import com.voltera.integracionami.domain.port.out.ConexionRepositoryPort;
import com.voltera.integracionami.domain.port.out.OutboxPort;
import com.voltera.integracionami.domain.port.out.SerializadorEventosPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
public class BeanConfiguration {

    @Bean
    public IntegracionAmiService integracionAmiService(ConexionRepositoryPort conexiones,
                                                       OutboxPort outbox,
                                                       SerializadorEventosPort serializador) {
        return new IntegracionAmiService(conexiones, outbox, serializador);
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper().registerModule(new JavaTimeModule());
    }
}
