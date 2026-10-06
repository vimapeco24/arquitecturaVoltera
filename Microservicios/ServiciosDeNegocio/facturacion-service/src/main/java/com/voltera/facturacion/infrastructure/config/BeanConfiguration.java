package com.voltera.facturacion.infrastructure.config;

import com.voltera.facturacion.application.FacturacionEventosService;
import com.voltera.facturacion.application.FacturacionService;
import com.voltera.facturacion.domain.port.out.FacturaRepositoryPort;
import com.voltera.facturacion.domain.port.out.OutboxPort;
import com.voltera.facturacion.domain.port.out.SerializadorEventosPort;
import com.voltera.facturacion.domain.port.out.TarifaConsultaPort;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Wiring de la arquitectura hexagonal: conecta el servicio de aplicacion
 * (agnostico de framework) con los adaptadores de salida inyectados por Spring
 * (persistencia + cliente HTTP hacia tarifas-service).
 * El dominio y la aplicacion no dependen de Spring; solo esta capa lo hace.
 */
@Configuration
@EnableScheduling
public class BeanConfiguration {

    @Bean
    public FacturacionService facturacionService(FacturaRepositoryPort repositorio,
                                                 TarifaConsultaPort tarifaConsulta) {
        return new FacturacionService(repositorio, tarifaConsulta);
    }

    @Bean
    public FacturacionEventosService facturacionEventosService(
            OutboxPort outbox,
            SerializadorEventosPort serializador,
            @Value("${facturacion.precio-kwh:0.5}") double precioKwh) {
        return new FacturacionEventosService(outbox, serializador, precioKwh);
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper().registerModule(new JavaTimeModule());
    }
}
