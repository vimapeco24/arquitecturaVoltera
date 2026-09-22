package com.voltera.facturacion.infrastructure.config;

import com.voltera.facturacion.application.FacturacionService;
import com.voltera.facturacion.domain.port.out.FacturaRepositoryPort;
import com.voltera.facturacion.domain.port.out.TarifaConsultaPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wiring de la arquitectura hexagonal: conecta el servicio de aplicacion
 * (agnostico de framework) con los adaptadores de salida inyectados por Spring
 * (persistencia + cliente HTTP hacia tarifas-service).
 * El dominio y la aplicacion no dependen de Spring; solo esta capa lo hace.
 */
@Configuration
public class BeanConfiguration {

    @Bean
    public FacturacionService facturacionService(FacturaRepositoryPort repositorio,
                                                 TarifaConsultaPort tarifaConsulta) {
        return new FacturacionService(repositorio, tarifaConsulta);
    }
}
