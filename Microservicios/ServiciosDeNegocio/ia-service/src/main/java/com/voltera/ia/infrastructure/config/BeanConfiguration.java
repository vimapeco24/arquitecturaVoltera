package com.voltera.ia.infrastructure.config;

import com.voltera.ia.application.CopilotoSiniestrosService;
import com.voltera.ia.application.DiagnosticoDerService;
import com.voltera.ia.domain.port.out.AgentRegistryPort;
import com.voltera.ia.domain.port.out.AuditLogPort;
import com.voltera.ia.domain.port.out.ConocimientoDerPort;
import com.voltera.ia.domain.port.out.ConocimientoPort;
import com.voltera.ia.domain.port.out.DiagnosticoEventPublisherPort;
import com.voltera.ia.domain.port.out.EventPublisherPort;
import com.voltera.ia.domain.port.out.GuardrailsPort;
import com.voltera.ia.domain.port.out.HerramientasDerPort;
import com.voltera.ia.domain.port.out.HerramientasPort;
import com.voltera.ia.domain.port.out.ModelGatewayPort;
import com.voltera.ia.domain.port.out.PromptModelRegistryPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;

/**
 * Wiring de la capa hexagonal: los servicios de aplicación son POJOs; aquí se
 * ensamblan con sus puertos de salida (adaptadores de infraestructura).
 */
@Configuration
public class BeanConfiguration {

    @Bean
    public CopilotoSiniestrosService copilotoSiniestrosService(
            ConocimientoPort conocimiento,
            HerramientasPort herramientas,
            ModelGatewayPort modelGateway,
            GuardrailsPort guardrails,
            EventPublisherPort eventPublisher,
            @Value("${ia.human-in-the-loop.umbral-aprobacion-humana:3000000}") BigDecimal umbral) {
        return new CopilotoSiniestrosService(
                conocimiento, herramientas, modelGateway, guardrails, eventPublisher, umbral);
    }

    /**
     * Caso del Grupo 5: Agente de diagnóstico de activos DER (flujo de 3 agentes,
     * cablea las 8 cajas marcadas del mapa de arquitectura de referencia).
     */
    @Bean
    public DiagnosticoDerService diagnosticoDerService(
            ConocimientoDerPort conocimientoDer,
            HerramientasDerPort herramientasDer,
            AgentRegistryPort agentRegistry,
            GuardrailsPort guardrails,
            ModelGatewayPort modelGateway,
            PromptModelRegistryPort promptRegistry,
            AuditLogPort auditLog,
            DiagnosticoEventPublisherPort diagnosticoEventPublisher) {
        return new DiagnosticoDerService(
                conocimientoDer, herramientasDer, agentRegistry, guardrails,
                modelGateway, promptRegistry, auditLog, diagnosticoEventPublisher);
    }
}
