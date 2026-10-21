package com.voltera.blockchain.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * CORS restringido a los origenes conocidos de Voltera (SPA en Vercel + local
 * de desarrollo). La lista se controla por la variable {@code VOLTERA_CORS_ORIGINS}
 * (patrones separados por coma); asi se evita el comodin "*" en produccion.
 *
 * <p>Solo se exponen los metodos y cabeceras que la API realmente usa, y se
 * desactivan las credenciales (la API es stateless, sin cookies de sesion).</p>
 */
@Configuration
public class WebCorsConfiguration implements WebMvcConfigurer {

    private final String[] origenesPermitidos;

    public WebCorsConfiguration(
            @Value("${voltera.cors.allowed-origins:"
                    + "https://voltera-frontend.vercel.app,"
                    + "https://*.vercel.app,"
                    + "http://localhost:4200}") String origenes) {
        this.origenesPermitidos = origenes.split("\\s*,\\s*");
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns(origenesPermitidos)
                .allowedMethods("GET", "POST", "OPTIONS")
                .allowedHeaders("Content-Type", "Accept", "Authorization")
                .allowCredentials(false)
                .maxAge(3600);
    }
}
