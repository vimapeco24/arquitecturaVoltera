package com.voltera.registry;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

/**
 * Service Registry de Voltera (Eureka Server).
 * Los microservicios de negocio (facturacion, liquidacion) se registran aqui
 * y se descubren entre si sin hardcodear direcciones.
 */
@SpringBootApplication
@EnableEurekaServer
public class ServiceRegistryApplication {
    public static void main(String[] args) {
        SpringApplication.run(ServiceRegistryApplication.class, args);
    }
}
