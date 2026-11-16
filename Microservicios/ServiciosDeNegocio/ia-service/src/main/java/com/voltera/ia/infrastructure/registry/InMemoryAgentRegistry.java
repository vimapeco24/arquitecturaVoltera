package com.voltera.ia.infrastructure.registry;

import com.voltera.ia.domain.port.out.AgentRegistryPort;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Caja 4 · Agent Registry (A2A). Catálogo en memoria de los cuatro agentes del
 * flujo de diagnóstico DER más el Assistant, con su dueño, capacidad y permisos
 * declarados. En producción sería un registro central consultable por otros
 * equipos; aquí basta para demostrar el patrón de descubrimiento/delegación y de
 * baja de agentes. Ninguno de ellos ejecuta comandos: el Agente-Acción solo
 * PROPONE (gobernanza 3/3) y el Assistant solo informa (sin herramientas).
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
                    "Propone comando correctivo u orden de trabajo y crea la propuesta 3/3 "
                            + "(NO ejecuta; requiere firmas de operador y prosumidor)",
                    "propuesta:accion, mcp:blockchain-gateway", true),
            new AgenteRegistrado("Agente-Notificador", "Equipo Operaciones de Red",
                    "Notifica a los firmantes y escala al mismo firmante pendiente ante timeout",
                    "notificaciones:enviar", true),
            new AgenteRegistrado("Assistant", "Equipo Operaciones de Red",
                    "Conversa sobre un diagnóstico antes de firmar (sin herramientas de acción; no persuade)",
                    "lectura:diagnostico", true));

    @Override
    public List<AgenteRegistrado> agentesActivos() {
        return AGENTES.stream().filter(AgenteRegistrado::activo).toList();
    }

    @Override
    public boolean habilitado(String nombreAgente) {
        return AGENTES.stream().anyMatch(a -> a.nombre().equalsIgnoreCase(nombreAgente) && a.activo());
    }
}
