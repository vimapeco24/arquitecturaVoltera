package com.voltera.tarifas.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuracion CORS global del servicio.
 *
 * Permite que un frontend (p. ej. desplegado en Vercel) consuma la API tanto
 * directamente como a traves del API Gateway. Los origenes permitidos se
 * controlan con la variable de entorno CORS_ALLOWED_ORIGINS (lista separada por
 * comas); por defecto se permite cualquier origen para el flujo demo.
 *
 * Se usa allowedOriginPatterns (no allowedOrigins) para poder combinar patrones
 * comodin con la respuesta reflejada del Origin. allowCredentials=false porque
 * la autenticacion viaja como Bearer token, no como cookie.
 */
@Configuration
public class WebCorsConfiguration implements WebMvcConfigurer {

    @Value("${CORS_ALLOWED_ORIGINS:*}")
    private String allowedOrigins;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns(allowedOrigins.split(","))
                .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("Location")
                .allowCredentials(false)
                .maxAge(3600);
    }
}
