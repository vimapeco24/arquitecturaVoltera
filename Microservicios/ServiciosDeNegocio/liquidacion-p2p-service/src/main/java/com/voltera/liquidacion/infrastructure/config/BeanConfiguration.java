package com.voltera.liquidacion.infrastructure.config;

import com.voltera.liquidacion.application.LiquidacionService;
import com.voltera.liquidacion.domain.port.out.TransaccionRepositoryPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wiring hexagonal: conecta el servicio de aplicacion con el adaptador
 * de persistencia. El dominio/aplicacion no dependen de Spring; solo esta capa.
 */
@Configuration
public class BeanConfiguration {

    @Bean
    public LiquidacionService liquidacionService(TransaccionRepositoryPort repositorio) {
        return new LiquidacionService(repositorio);
    }
}
