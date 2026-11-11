package com.voltera.ia.infrastructure.registry;

import com.voltera.ia.domain.port.out.AgentRegistryPort;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Caja 4 · Agent Registry (A2A). Catálogo en memoria de los tres agentes del
 * flujo de diagnóstico DER, con su dueño, capacidad y permisos declarados. En
 * producción sería un registro central consultable por otros equipos; aquí basta
 * para demostrar el patrón de descubrimiento/delegación y de baja de agentes.
 */
@Component
public class InMemoryAgentRegistry implements AgentRegistryPort {

    private static final List<AgenteRegistrado> AGENTES = List.of(
            new AgenteRegistrado("Agente-Monitor", "Equipo Operaciones de Red",
                    "Evalúa umbrales adaptativos sobre la telemetría entrante",
                    "lectura:telemetria", true),
            new AgenteRegistrado("Agente-Diagnosticador", "Equipo Operaciones de Red",
                    "Correlaciona la anomalía con RAG y herramientas MCP; clasifica severidad",
                    "lectura:rag, lectura:mcp", true),
            new AgenteRegistrado("Agente-Accion", "Equipo Operaciones de Red",
                    "Propone comando correctivo u orden de trabajo (NO ejecuta; requiere Human Review)",
                    "propuesta:accion", true));

    @Override
    public List<AgenteRegistrado> agentesActivos() {
        return AGENTES.stream().filter(AgenteRegistrado::activo).toList();
    }

    @Override
    public boolean habilitado(String nombreAgente) {
        return AGENTES.stream().anyMatch(a -> a.nombre().equalsIgnoreCase(nombreAgente) && a.activo());
    }
}
