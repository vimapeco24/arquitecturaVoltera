package com.voltera.ia.application;

import com.voltera.ia.domain.model.Evidencia;
import com.voltera.ia.domain.model.asistente.Audiencia;
import com.voltera.ia.domain.model.asistente.PreguntaAsistente;
import com.voltera.ia.domain.model.asistente.RespuestaAsistente;
import com.voltera.ia.domain.model.der.DiagnosticoDer;
import com.voltera.ia.domain.port.in.AsistenteUseCase;
import com.voltera.ia.domain.port.out.DiagnosticoRepositoryPort;
import com.voltera.ia.domain.port.out.GuardrailsPort;
import com.voltera.ia.domain.port.out.PromptModelRegistryPort;

import java.util.List;

/**
 * Assistant conversacional acotado al diagnóstico, previo a la firma (opción B
 * del documento). Responde SOLO con el diagnóstico, la propuesta y la evidencia
 * RAG de ese caso concreto. Propiedades que impone el documento:
 *
 * <ul>
 *   <li><b>Sin herramientas de acción</b>: no firma, no crea propuestas, no
 *       ejecuta comandos. La firma es un control separado de la conversación.</li>
 *   <li><b>Prompts por audiencia</b>: prosumidor (simple) vs operador (técnico),
 *       versionados en el Prompt &amp; Model Registry (Tema 5).</li>
 *   <li><b>Guardrail de no persuasión</b>: presenta de forma neutral las
 *       consecuencias de aprobar y de rechazar; nunca induce a aprobar.</li>
 * </ul>
 */
public class AsistenteService implements AsistenteUseCase {

    private final DiagnosticoRepositoryPort diagnosticos;
    private final GuardrailsPort guardrails;
    private final PromptModelRegistryPort promptRegistry;

    private com.voltera.ia.domain.port.out.AgentRegistryPort agentRegistry;

    public AsistenteService(DiagnosticoRepositoryPort diagnosticos, GuardrailsPort guardrails,
                            PromptModelRegistryPort promptRegistry) {
        this.diagnosticos = diagnosticos;
        this.guardrails = guardrails;
        this.promptRegistry = promptRegistry;
    }

    /** Respeta las bajas del Agent Registry: un Assistant dado de baja no responde. */
    public AsistenteService conRegistro(com.voltera.ia.domain.port.out.AgentRegistryPort agentRegistry) {
        this.agentRegistry = agentRegistry;
        return this;
    }

    @Override
    public RespuestaAsistente responder(PreguntaAsistente pregunta) {
        if (agentRegistry != null && !agentRegistry.habilitado("Assistant")) {
            throw new com.voltera.ia.domain.exception.AgenteDeBajaException("Assistant");
        }
        // Guardrail de entrada: rechaza inyección de prompt.
        guardrails.validarEntrada(pregunta.texto());

        DiagnosticoDer d = diagnosticos.porId(pregunta.diagnosticoId()).orElseThrow(
                () -> new IllegalArgumentException(
                        "El Assistant solo conversa sobre un diagnóstico existente: "
                                + pregunta.diagnosticoId()));

        Audiencia audiencia = pregunta.audiencia() == null ? Audiencia.PROSUMIDOR : pregunta.audiencia();
        String agentePrompt = audiencia == Audiencia.OPERADOR ? "asistente-operador" : "asistente-prosumidor";
        String promptVersion = promptRegistry.versionPrompt(agentePrompt);

        // Guardrail de alcance: solo se conversa sobre este diagnóstico.
        if (guardrails.fueraDeAlcance(pregunta.texto())) {
            String alcance = "Solo puedo responder preguntas sobre este diagnóstico del activo " + d.activoId()
                    + ": qué se detectó, con qué evidencia, qué acción se propone y qué pasa si apruebas o si rechazas.";
            return RespuestaAsistente.de(d.diagnosticoId().toString(), alcance, List.of(), promptVersion, "FUERA_DE_ALCANCE", true);
        }

        String intencion = intencion(pregunta.texto());
        String base = audiencia == Audiencia.OPERADOR
                ? redactarOperador(d, pregunta.texto())
                : redactarProsumidor(d, pregunta.texto());
        String cuerpo = responderIntencion(d, intencion, audiencia) + " " + base;

        // Guardrail de salida: enmascara PII y neutraliza cualquier persuasión.
        String seguro = guardrails.neutralizarPersuasion(guardrails.enmascararPII(cuerpo));

        return RespuestaAsistente.de(d.diagnosticoId().toString(), seguro, d.evidencias(), promptVersion, intencion, false);
    }

    // ------------------------------------------------------------- intención

    /** Clasifica la pregunta por palabras clave (determinista y auditable). */
    static String intencion(String texto) {
        String t = texto == null ? "" : texto.toLowerCase();
        if (t.matches(".*(por qu|evidencia|fuente|c[oó]mo sab|en qu[eé] se basa|prueba).*")) return "EVIDENCIA";
        if (t.matches(".*(cu[aá]nto tarda|cu[aá]ndo|tiempo|demora|plazo|vence).*")) return "TIEMPO";
        if (t.matches(".*(cu[aá]nto cuesta|costo|cobr|pagar|factura|precio).*")) return "COSTO";
        if (t.matches(".*(peligro|seguro|riesgo|incendio|da[nñ]o).*")) return "SEGURIDAD";
        if (t.matches(".*(qui[eé]n firma|firmas|proceso|c[oó]mo funciona|pasos|3/3).*")) return "PROCESO";
        if (t.matches(".*(qu[eé] pasa|si no apruebo|si rechazo|si apruebo|consecuencia).*")) return "CONSECUENCIAS";
        return "GENERAL";
    }

    private String responderIntencion(DiagnosticoDer d, String intencion, Audiencia aud) {
        boolean simple = aud != Audiencia.OPERADOR;
        return switch (intencion) {
            case "EVIDENCIA" -> "La conclusión se apoya en " + d.evidencias().size() + " fuentes: "
                    + String.join("; ", d.evidencias().stream().limit(4).map(e -> e.titulo()).toList()) + ".";
            case "TIEMPO" -> simple
                    ? "La propuesta tiene un plazo para reunir las tres firmas; si vence, no se hace nada sobre tu equipo."
                    : "La propuesta vence según su TTL (crítica 30 min, resto 4 h); vencida sin 3/3 queda VENCIDA y no se ejecuta.";
            case "COSTO" -> simple
                    ? "Este asistente no calcula cobros. Lo que sí se sabe: con el equipo fuera de rango pueden quedar lecturas de energía erróneas que afectan la liquidación."
                    : "El diagnóstico no estima costos. El fallo conocido asociado indica riesgo de lecturas erróneas que afectan la liquidación y la facturación.";
            case "SEGURIDAD" -> "La severidad registrada es " + d.severidad() + ". "
                    + (d.accionPropuesta() == com.voltera.ia.domain.model.der.TipoAccion.DESPACHAR_TECNICO
                        ? "Se propone una visita técnica porque la causa puede requerir inspección física."
                        : "Ninguna acción se ejecuta sobre el equipo sin las tres firmas.");
            case "PROCESO" -> "Firman tres partes: el agente IA al proponer, el operador al validar técnicamente y el prosumidor al consentir. Sin las tres no se ejecuta nada.";
            case "CONSECUENCIAS" -> "Sobre tu pregunta:";
            default -> "";
        };
    }

    // ------------------------------------------------------------- redacción

    private String redactarProsumidor(DiagnosticoDer d, String pregunta) {
        StringBuilder sb = new StringBuilder();
        sb.append("Sobre tu dispositivo ").append(d.activoId()).append(": ")
          .append(resumenSimple(d)).append(" ");
        sb.append("Se propone esta acción: ").append(accionSimple(d)).append(". ");
        sb.append(consecuenciasNeutrales(d));
        sb.append(" Esta es tu decisión: puedes aprobar o rechazar; el sistema no decide por ti ")
          .append("y yo solo te explico, no firmo ni ejecuto nada.");
        return sb.toString();
    }

    private String redactarOperador(DiagnosticoDer d, String pregunta) {
        StringBuilder sb = new StringBuilder();
        sb.append("Diagnóstico ").append(d.diagnosticoId()).append(" · activo ").append(d.activoId())
          .append(" (").append(d.tipoActivo()).append(", comunidad ").append(d.comunidadId()).append("). ");
        sb.append("Severidad ").append(d.severidad()).append(". ");
        sb.append(d.diagnostico()).append(" ");
        sb.append("Acción propuesta: ").append(d.accionPropuesta())
          .append(d.requiereAprobacionHumana() ? " (requiere firma 2/3 del operador). " : ". ");
        sb.append("Evidencia citada: ").append(d.evidencias().size())
          .append(" fuentes (RAG + MCP). ");
        sb.append("Prompt del diagnóstico: ").append(d.promptVersion())
          .append(", motor ").append(d.motor()).append(". ");
        sb.append(consecuenciasNeutrales(d));
        return sb.toString();
    }

    private String resumenSimple(DiagnosticoDer d) {
        return switch (d.severidad()) {
            case CRITICA -> "detectamos un problema importante que conviene atender pronto.";
            case ADVERTENCIA -> "detectamos algo que vale la pena revisar.";
            case INFO -> "todo parece estar en orden; es solo seguimiento.";
        };
    }

    private String accionSimple(DiagnosticoDer d) {
        return switch (d.accionPropuesta()) {
            case COMANDO_FISICO -> "ejecutar una acción sobre el equipo (por ejemplo reiniciar el inversor)";
            case CORTAR_SUMINISTRO -> "limitar temporalmente el suministro del equipo";
            case DESPACHAR_TECNICO -> "enviar a un técnico a revisar el equipo";
            case MONITOREAR -> "seguir observando, sin tocar el equipo";
        };
    }

    /** Presenta de forma NEUTRAL las consecuencias de aprobar y de rechazar. */
    private String consecuenciasNeutrales(DiagnosticoDer d) {
        return "Si apruebas, la acción se ejecuta solo después de reunir las tres firmas "
                + "(sistema, operador y tú). Si rechazas, no se hace nada sobre el equipo y queda "
                + "registrada tu decisión con el motivo. Ambas opciones son válidas.";
    }
}
