package com.voltera.pagos.infrastructure.config;

import com.voltera.pagos.application.PagoService;
import com.voltera.pagos.domain.port.out.PagoRepositoryPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BeanConfiguration {

    @Bean
    public PagoService pagoService(PagoRepositoryPort repositorio) {
        return new PagoService(repositorio);
    }
}
