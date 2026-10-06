package com.voltera.tarifas.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.voltera.tarifas.application.AsignacionTarifaService;
import com.voltera.tarifas.application.TarifaService;
import com.voltera.tarifas.domain.port.out.OutboxPort;
import com.voltera.tarifas.domain.port.out.SerializadorEventosPort;
import com.voltera.tarifas.domain.port.out.TarifaRepositoryPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
public class BeanConfiguration {

    @Bean
    public TarifaService tarifaService(TarifaRepositoryPort repositorio) {
        return new TarifaService(repositorio);
    }

    @Bean
    public AsignacionTarifaService asignacionTarifaService(TarifaRepositoryPort repositorio,
                                                           OutboxPort outbox,
                                                           SerializadorEventosPort serializador) {
        return new AsignacionTarifaService(repositorio, outbox, serializador);
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper().registerModule(new JavaTimeModule());
    }
}
