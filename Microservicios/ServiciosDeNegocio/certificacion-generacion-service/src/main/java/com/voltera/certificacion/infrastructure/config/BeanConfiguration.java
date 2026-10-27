package com.voltera.certificacion.infrastructure.config;

import com.voltera.certificacion.application.CertificacionService;
import com.voltera.certificacion.domain.port.out.SolicitudRepositoryPort;
import com.voltera.certificacion.infrastructure.persistence.InMemorySolicitudRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Wiring hexagonal. El dominio/aplicación no conocen Spring; solo esta capa. */
@Configuration
public class BeanConfiguration {

    @Bean
    public SolicitudRepositoryPort solicitudRepository() {
        return new InMemorySolicitudRepository();
    }

    @Bean
    public CertificacionService certificacionService(
            SolicitudRepositoryPort repo,
            @Value("${voltera.certificacion.bloque-wh:1000000}") long bloqueWh) {
        return new CertificacionService(repo, bloqueWh);
    }
}
