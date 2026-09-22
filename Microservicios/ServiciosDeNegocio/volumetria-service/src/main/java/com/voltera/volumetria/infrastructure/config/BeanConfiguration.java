package com.voltera.volumetria.infrastructure.config;

import com.voltera.volumetria.application.VolumetriaService;
import com.voltera.volumetria.domain.port.out.LecturaRepositoryPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BeanConfiguration {

    @Bean
    public VolumetriaService volumetriaService(LecturaRepositoryPort repositorio) {
        return new VolumetriaService(repositorio);
    }
}
