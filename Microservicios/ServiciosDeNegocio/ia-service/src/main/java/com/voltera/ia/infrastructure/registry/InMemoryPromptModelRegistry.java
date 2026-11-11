package com.voltera.ia.infrastructure.registry;

import com.voltera.ia.domain.port.out.PromptModelRegistryPort;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Caja 6 · Prompt &amp; Model Registry. Versiones de prompts por agente. Un prompt
 * es código: se versiona y se despliega, no se edita en caliente. Aquí se fija la
 * versión en producción de cada agente y su plantilla; cambiar de versión es una
 * operación de registro (rollback incluido), no una edición dentro del prompt.
 */
@Component
public class InMemoryPromptModelRegistry implements PromptModelRegistryPort {

    private static final Map<String, String> VERSIONES = Map.of(
            "monitor", "monitor@v1",
            "diagnosticador", "diagnosticador@v3",
            "accion", "accion@v2");

    private static final Map<String, String> PLANTILLAS = Map.of(
            "diagnosticador",
            "Eres el Agente-Diagnosticador de activos DER de Voltera. A partir de la alerta y de la "
                    + "evidencia recuperada (fichas técnicas, fallos conocidos, telemetría MCP), redacta un "
                    + "diagnóstico breve en español con el nivel de severidad y cita la evidencia. No decides "
                    + "acciones sobre infraestructura: eso lo aprueba una persona.");

    @Override
    public String versionPrompt(String agente) {
        return VERSIONES.getOrDefault(agente, agente + "@v1");
    }

    @Override
    public String promptActivo(String agente) {
        return PLANTILLAS.getOrDefault(agente, "");
    }
}
