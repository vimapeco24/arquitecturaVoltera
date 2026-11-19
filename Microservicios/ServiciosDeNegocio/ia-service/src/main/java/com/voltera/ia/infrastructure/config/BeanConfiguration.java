package com.voltera.ia.infrastructure.config;

import com.voltera.ia.application.CopilotoSiniestrosService;
import com.voltera.ia.application.DiagnosticoDerService;
import com.voltera.ia.application.AsistenteService;
import com.voltera.ia.application.EvalsService;
import com.voltera.ia.application.GobernanzaAccionService;
import com.voltera.ia.domain.port.out.AgentRegistryPort;
import com.voltera.ia.domain.port.out.AuditLogPort;
import com.voltera.ia.domain.port.out.ConocimientoDerPort;
import com.voltera.ia.domain.port.out.ConocimientoPort;
import com.voltera.ia.domain.port.out.DiagnosticoEventPublisherPort;
import com.voltera.ia.domain.port.out.DiagnosticoRepositoryPort;
import com.voltera.ia.domain.port.out.EventPublisherPort;
import com.voltera.ia.domain.port.out.GobernanzaEventPublisherPort;
import com.voltera.ia.domain.port.out.GuardrailsPort;
import com.voltera.ia.domain.port.out.HerramientasDerPort;
import com.voltera.ia.domain.port.out.HerramientasPort;
import com.voltera.ia.domain.port.out.McpBlockchainGatewayPort;
import com.voltera.ia.domain.port.out.ModelGatewayPort;
import com.voltera.ia.domain.port.out.NotificadorPort;
import com.voltera.ia.domain.port.out.PromptModelRegistryPort;
import com.voltera.ia.domain.port.out.PropuestaRepositoryPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.time.Duration;

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
            DiagnosticoEventPublisherPort diagnosticoEventPublisher,
            DiagnosticoRepositoryPort diagnosticoRepositorio) {
        return new DiagnosticoDerService(
                conocimientoDer, herramientasDer, agentRegistry, guardrails,
                modelGateway, promptRegistry, auditLog, diagnosticoEventPublisher,
                diagnosticoRepositorio);
    }

    /**
     * Gobernanza multi-firma 3/3 sobre acciones correctivas de DER. Reúne el MCP
     * Blockchain Gateway (contrato MultiFirmaAccionDER), el Agente Notificador y
     * el tópico der.gobernanza. El TTL por criticidad lo marca el Rules Engine.
     */
    @Bean
    public GobernanzaAccionService gobernanzaAccionService(
            McpBlockchainGatewayPort gateway,
            NotificadorPort notificador,
            PropuestaRepositoryPort propuestaRepositorio,
            GobernanzaEventPublisherPort gobernanzaEventPublisher,
            @Value("${ia.gobernanza.ttl-critica-min:30}") long ttlCriticaMin,
            @Value("${ia.gobernanza.ttl-normal-min:240}") long ttlNormalMin,
            @Value("${ia.gobernanza.explorer-base:https://amoy.polygonscan.com}") String explorerBase) {
        return new GobernanzaAccionService(
                gateway, notificador, propuestaRepositorio, gobernanzaEventPublisher,
                Duration.ofMinutes(ttlCriticaMin), Duration.ofMinutes(ttlNormalMin), explorerBase);
    }

    /**
     * Assistant conversacional acotado al diagnóstico, previo a la firma. Sin
     * herramientas de acción; prompts por audiencia; guardrail de no persuasión.
     */
    @Bean
    public AsistenteService asistenteService(
            DiagnosticoRepositoryPort diagnosticoRepositorio,
            GuardrailsPort guardrails,
            PromptModelRegistryPort promptRegistry) {
        return new AsistenteService(diagnosticoRepositorio, guardrails, promptRegistry);
    }

    /** Evals & Tracing (Tema 5): compuerta de calidad de cada versión de prompt. */
    @Bean
    public EvalsService evalsService(PromptModelRegistryPort promptRegistry) {
        return new EvalsService(promptRegistry);
    }
}
