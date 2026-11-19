package com.voltera.ia.infrastructure.config;

import com.voltera.ia.infrastructure.observabilidad.ObservabilidadInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Registra el interceptor que mide cada funcionalidad del módulo de IA. */
@Configuration
public class ObservabilidadWebConfig implements WebMvcConfigurer {

    private final ObservabilidadInterceptor interceptor;

    public ObservabilidadWebConfig(ObservabilidadInterceptor interceptor) {
        this.interceptor = interceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(interceptor).addPathPatterns("/api/v1/ia/**");
    }
}
