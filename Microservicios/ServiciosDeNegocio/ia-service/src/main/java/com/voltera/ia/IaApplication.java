package com.voltera.ia;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Plataforma GenAI de Voltera. Capa transversal que razona sobre los
 * microservicios (herramientas), los eventos (disparadores) y la cadena
 * (fuente de verdad). No reemplaza ningun dominio: los orquesta.
 */
@SpringBootApplication
public class IaApplication {
    public static void main(String[] args) {
        SpringApplication.run(IaApplication.class, args);
    }
}
