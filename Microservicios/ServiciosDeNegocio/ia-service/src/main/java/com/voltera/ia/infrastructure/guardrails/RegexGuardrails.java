package com.voltera.ia.infrastructure.guardrails;

import com.voltera.ia.domain.exception.GuardrailViolationException;
import com.voltera.ia.domain.port.out.GuardrailsPort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Guardrails declarativos (politicas versionadas, no instrucciones dentro del
 * prompt). Implementacion ligera basada en patrones: enmascara PII y detecta
 * intentos basicos de inyeccion de prompt / temas fuera de alcance.
 *
 * <p>Es un PII Detector / Safety Model de demostracion: suficiente para mostrar
 * el patron arquitectonico sin dependencias externas. En produccion se sustituye
 * por un clasificador dedicado sin tocar al agente (esta detras del puerto).</p>
 */
@Component
public class RegexGuardrails implements GuardrailsPort {

    private final java.util.Map<String, java.util.concurrent.atomic.AtomicLong> activaciones = new java.util.concurrent.ConcurrentHashMap<>();

    /** Catálogo de políticas declaradas y versionadas (id, tipo, versión, descripción, acción). */
    private static final List<String[]> CATALOGO = List.of(
            new String[]{"ENT-INYECCION", "ENTRADA", "v2", "Detecta frases de inyección de prompt (ignora instrucciones, actúa como, revela el system prompt…).", "BLOQUEAR (HTTP 422)"},
            new String[]{"ENT-PII", "ENTRADA", "v1", "Enmascara correos y números de 6 o más dígitos (documentos, cuentas, teléfonos) antes de cualquier modelo o registro.", "ENMASCARAR"},
            new String[]{"ENT-ALCANCE", "ENTRADA", "v1", "El Assistant solo responde sobre el diagnóstico: preguntas de otros temas reciben una respuesta de alcance.", "RESPONDER ALCANCE"},
            new String[]{"SAL-PII", "SALIDA", "v1", "Vuelve a enmascarar PII en toda salida (diagnóstico, Assistant, auditoría).", "ENMASCARAR"},
            new String[]{"SAL-PERSUASION", "SALIDA", "v1", "Neutraliza frases que inducen a firmar (no persuasión del Assistant).", "NEUTRALIZAR"},
            new String[]{"SAL-FORMATO", "SALIDA", "v1", "La redacción de un LLM debe nombrar severidad y acción; si no, se usa la redacción local.", "RESPALDO LOCAL"});

    @Override
    public void registrarActivacion(String politicaId) {
        activaciones.computeIfAbsent(politicaId, k -> new java.util.concurrent.atomic.AtomicLong()).incrementAndGet();
    }

    @Override
    public List<Politica> politicas() {
        return CATALOGO.stream().map(c -> new Politica(c[0], c[1], c[2], c[3], c[4],
                activaciones.getOrDefault(c[0], new java.util.concurrent.atomic.AtomicLong()).get())).toList();
    }

    /** Temas que el Assistant no atiende. */
    private static final List<String> FUERA_DE_ALCANCE = List.of(
            "receta", "futbol", "fútbol", "partido", "chiste", "politic", "elecciones", "bitcoin", "criptomoneda",
            "acciones de la bolsa", "horoscopo", "horóscopo", "pelicula", "película", "tarea de", "poema", "cancion", "canción");
    /** Vocabulario del caso: si no aparece ninguno, la pregunta no es sobre el diagnóstico. */
    private static final List<String> DOMINIO = List.of(
            "aprob", "aprueb", "rechaz", "firm", "hola", "explica", "entiendo", "ayuda", "significa", "riesgo", "pasa", "equipo", "activo", "inversor", "bateri", "batería", "panel",
            "medidor", "cargador", "tecnic", "técnic", "evidencia", "por que", "por qué", "porque", "severidad", "accion",
            "acción", "costo", "cuesta", "tiempo", "tarda", "cuando", "cuándo", "energ", "factura", "diagn", "comando",
            "reinici", "segur", "peligro", "consecuencia", "quien", "quién", "prosumidor", "operador", "dato", "fuente");

    @Override
    public boolean fueraDeAlcance(String texto) {
        if (texto == null || texto.isBlank()) return false;
        String l = texto.toLowerCase();
        boolean tema = FUERA_DE_ALCANCE.stream().anyMatch(l::contains);
        boolean dominio = DOMINIO.stream().anyMatch(l::contains);
        boolean fuera = tema || (!dominio && l.trim().split("\\s+").length >= 3);
        if (fuera) registrarActivacion("ENT-ALCANCE");
        return fuera;
    }

    // Correos, telefonos largos y numeros de documento/cuenta (>= 6 digitos).
    private static final Pattern EMAIL = Pattern.compile("[\\w.+-]+@[\\w.-]+\\.[A-Za-z]{2,}");
    private static final Pattern DIGITOS_LARGOS = Pattern.compile("\\b\\d{6,}\\b");

    private static final List<String> FRASES_INYECCION = List.of(
            "ignore previous", "ignora las instrucciones", "olvida las instrucciones",
            "actua como", "system prompt", "revela tus instrucciones", "jailbreak");

    @Override
    public String enmascararPII(String texto) {
        if (texto == null || texto.isBlank()) return "";
        String out = EMAIL.matcher(texto).replaceAll("[EMAIL]");
        out = DIGITOS_LARGOS.matcher(out).replaceAll("[ID]");
        if (!out.equals(texto)) registrarActivacion("ENT-PII");
        return out;
    }

    @Override
    public void validarEntrada(String texto) {
        if (texto == null) return;
        String lower = texto.toLowerCase();
        for (String frase : FRASES_INYECCION) {
            if (lower.contains(frase)) {
                registrarActivacion("ENT-INYECCION");
                throw new GuardrailViolationException(
                        "Guardrail de entrada: posible inyeccion de prompt detectada ('" + frase + "').");
            }
        }
    }

    // Frases que inducen al firmante a aprobar: el Assistant NO debe persuadir.
    private static final List<String> FRASES_PERSUASION = List.of(
            "deberías aprobar", "deberias aprobar", "te conviene aprobar", "te conviene firmar",
            "lo mejor es aprobar", "recomiendo aprobar", "deberías firmar", "deberias firmar",
            "tienes que aprobar", "apruébalo", "apruebalo", "firma ya", "no rechaces");

    @Override
    public String neutralizarPersuasion(String texto) {
        if (texto == null || texto.isBlank()) return "";
        String out = texto;
        for (String frase : FRASES_PERSUASION) {
            if (out.toLowerCase().contains(frase)) registrarActivacion("SAL-PERSUASION");
            out = out.replaceAll("(?i)" + Pattern.quote(frase),
                    "[el Assistant no recomienda firmar; la decisión es suya]");
        }
        return out;
    }
}
