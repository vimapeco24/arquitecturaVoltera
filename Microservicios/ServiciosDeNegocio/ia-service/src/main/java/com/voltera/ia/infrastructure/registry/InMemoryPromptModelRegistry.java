package com.voltera.ia.infrastructure.registry;

import com.voltera.ia.domain.port.out.PromptModelRegistryPort;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Caja 6 · Prompt &amp; Model Registry. Cada familia tiene sus versiones con estado
 * (PRODUCCION, CANDIDATA, RETIRADA), quién aprobó y cuándo. Promover y revertir son
 * operaciones del registro con historial, no ediciones dentro del prompt. La compuerta
 * de calidad (Evals) la aplica {@code RegistroPromptsService} antes de promover.
 */
@Component
public class InMemoryPromptModelRegistry implements PromptModelRegistryPort {

    private static final Instant INICIAL = Instant.parse("2026-11-01T12:00:00Z");
    private static final String COMITE = "comite-ia@voltera";

    private final Map<String, List<VersionPrompt>> familias = new LinkedHashMap<>();
    private final Map<String, String> activa = new LinkedHashMap<>();
    private final Map<String, List<String>> pilaAnteriores = new LinkedHashMap<>();
    private final List<CambioVersion> historial = Collections.synchronizedList(new ArrayList<>());

    public InMemoryPromptModelRegistry() {
        reg("monitor", "monitor@v1", PRODUCCION, "Umbrales: valor > umbral o ≥ 15 min sin señal.",
                "Eres el Agente-Monitor: decide si la lectura es anómala según el umbral del activo.");
        reg("diagnosticador", "diagnosticador@v2", RETIRADA, "Clasificación sin evidencia MCP (retirada).",
                "Eres el Agente-Diagnosticador. Clasifica la severidad.");
        reg("diagnosticador", "diagnosticador@v3", PRODUCCION,
                "Severidad por desvío respecto al umbral; compara siempre valor > umbral.",
                "Eres el Agente-Diagnosticador de activos DER de Voltera. A partir de la alerta y de la "
                        + "evidencia recuperada (fichas técnicas, fallos conocidos, telemetría MCP), redacta un "
                        + "diagnóstico breve en español con el nivel de severidad y cita la evidencia. No decides "
                        + "acciones sobre infraestructura: eso lo aprueba una persona.");
        reg("diagnosticador", "diagnosticador@v4", CANDIDATA,
                "Agrega métricas donde menor es peor (SoH, SoC) con límites críticos de la ficha técnica "
                        + "(SoH < 70 %, SoC < 10 %). Corrige los fallos EV-19 y EV-20 de las Evals.",
                "Eres el Agente-Diagnosticador de activos DER de Voltera. Considera la dirección de cada "
                        + "métrica: en SoH y SoC un valor menor al umbral es degradación. Cita la evidencia y "
                        + "redacta en español; no decides acciones sobre infraestructura.");
        reg("accion", "accion@v2", PRODUCCION, "Acción por severidad; toda acción física exige firma 3/3.",
                "Eres el Agente-Acción: propones, nunca ejecutas.");
        reg("notificador", "notificador@v1", PRODUCCION, "Aviso por etapa y escalación al mismo firmante.",
                "Eres el Agente-Notificador: avisa al firmante pendiente con lenguaje de su audiencia.");
        reg("asistente-prosumidor", "asistente-prosumidor@v2", PRODUCCION, "Lenguaje simple, neutral, sin persuadir.",
                "Explica al prosumidor el diagnóstico de su equipo sin recomendar firmar.");
        reg("asistente-operador", "asistente-operador@v2", PRODUCCION, "Lenguaje técnico con evidencia citada.",
                "Explica al operador el diagnóstico con evidencia y versión de prompt.");
    }

    private void reg(String familia, String version, String estado, String desc, String plantilla) {
        familias.computeIfAbsent(familia, k -> new ArrayList<>())
                .add(new VersionPrompt(familia, version, estado, desc,
                        PRODUCCION.equals(estado) || RETIRADA.equals(estado) ? COMITE : null,
                        PRODUCCION.equals(estado) || RETIRADA.equals(estado) ? INICIAL : null, plantilla));
        if (PRODUCCION.equals(estado)) activa.put(familia, version);
    }

    @Override
    public synchronized String versionPrompt(String agente) {
        return activa.getOrDefault(agente, agente + "@v1");
    }

    @Override
    public synchronized String promptActivo(String agente) {
        String v = activa.get(agente);
        return versiones(agente).stream().filter(x -> x.version().equals(v)).map(VersionPrompt::plantilla)
                .findFirst().orElse("");
    }

    @Override
    public synchronized List<String> familias() {
        return List.copyOf(familias.keySet());
    }

    @Override
    public synchronized List<VersionPrompt> versiones(String familia) {
        return List.copyOf(familias.getOrDefault(familia, List.of()));
    }

    @Override
    public synchronized VersionPrompt promover(String familia, String version, String responsable, String motivo) {
        List<VersionPrompt> vs = familias.get(familia);
        if (vs == null) throw new IllegalArgumentException("Familia de prompt desconocida: " + familia);
        VersionPrompt objetivo = vs.stream().filter(v -> v.version().equals(version)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Versión desconocida: " + version));
        String actual = activa.get(familia);
        if (version.equals(actual)) throw new IllegalStateException(version + " ya está en producción.");
        pilaAnteriores.computeIfAbsent(familia, k -> new ArrayList<>()).add(actual);
        cambiar(familia, actual, version, responsable);
        historial.add(new CambioVersion(Instant.now(), familia, actual, version, "PROMOCION", responsable, motivo));
        return vs.stream().filter(v -> v.version().equals(objetivo.version())).findFirst().orElseThrow();
    }

    @Override
    public synchronized VersionPrompt revertir(String familia, String responsable, String motivo) {
        List<String> pila = pilaAnteriores.getOrDefault(familia, new ArrayList<>());
        if (pila.isEmpty()) throw new IllegalStateException("No hay una versión anterior a la que volver en " + familia + ".");
        String anterior = pila.remove(pila.size() - 1);
        String actual = activa.get(familia);
        cambiar(familia, actual, anterior, responsable);
        historial.add(new CambioVersion(Instant.now(), familia, actual, anterior, "ROLLBACK", responsable, motivo));
        return familias.get(familia).stream().filter(v -> v.version().equals(anterior)).findFirst().orElseThrow();
    }

    private void cambiar(String familia, String desde, String hacia, String responsable) {
        List<VersionPrompt> vs = familias.get(familia);
        for (int i = 0; i < vs.size(); i++) {
            VersionPrompt v = vs.get(i);
            if (v.version().equals(hacia)) {
                vs.set(i, new VersionPrompt(familia, v.version(), PRODUCCION, v.descripcion(), responsable, Instant.now(), v.plantilla()));
            } else if (v.version().equals(desde)) {
                vs.set(i, new VersionPrompt(familia, v.version(), RETIRADA, v.descripcion(), v.aprobadaPor(), v.aprobadaEn(), v.plantilla()));
            }
        }
        activa.put(familia, hacia);
    }

    @Override
    public List<CambioVersion> historial() {
        synchronized (historial) {
            List<CambioVersion> out = new ArrayList<>(historial);
            Collections.reverse(out);
            return out;
        }
    }
}
