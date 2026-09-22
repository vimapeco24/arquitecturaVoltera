package com.voltera.liquidacionmensual.infrastructure.config;

import com.voltera.liquidacionmensual.application.LiquidacionMensualService;
import com.voltera.liquidacionmensual.domain.port.out.LiquidacionRepositoryPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BeanConfiguration {

    @Bean
    public LiquidacionMensualService liquidacionMensualService(LiquidacionRepositoryPort repositorio) {
        return new LiquidacionMensualService(repositorio);
    }
}
