package com.voltera.tarifas.infrastructure.config;

import com.voltera.tarifas.application.TarifaService;
import com.voltera.tarifas.domain.port.out.TarifaRepositoryPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BeanConfiguration {

    @Bean
    public TarifaService tarifaService(TarifaRepositoryPort repositorio) {
        return new TarifaService(repositorio);
    }
}
