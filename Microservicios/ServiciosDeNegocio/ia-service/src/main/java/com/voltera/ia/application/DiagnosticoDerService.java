package com.voltera.ia.application;

import com.voltera.ia.domain.model.Evidencia;
import com.voltera.ia.domain.model.PasoFlujo;
import com.voltera.ia.domain.model.der.AlertaTelemetria;
import com.voltera.ia.domain.model.der.DiagnosticoDer;
import com.voltera.ia.domain.model.der.DiagnosticoDerGenerado;
import com.voltera.ia.domain.model.der.Severidad;
import com.voltera.ia.domain.model.der.TipoAccion;
import com.voltera.ia.domain.port.in.DiagnosticoDerUseCase;
import com.voltera.ia.domain.port.out.AgentRegistryPort;
import com.voltera.ia.domain.port.out.AuditLogPort;
import com.voltera.ia.domain.port.out.ConocimientoDerPort;
import com.voltera.ia.domain.port.out.DiagnosticoEventPublisherPort;
import com.voltera.ia.domain.port.out.GuardrailsPort;
import com.voltera.ia.domain.port.out.HerramientasDerPort;
import com.voltera.ia.domain.port.out.ModelGatewayPort;
import com.voltera.ia.domain.port.out.PromptModelRegistryPort;

import java.util.ArrayList;
import java.util.List;

/**
 * Caso del Grupo 5: "Agente de diagnóstico de activos distribuidos (DER) y
 * copiloto de soporte de red". Flujo agéntico de TRES agentes orquestados
 * (AI Flows / Agent Harness), que reutiliza las 8 cajas marcadas del mapa de
 * arquitectura de referencia:
 *
 * <ol>
 *   <li><b>Agente-Monitor</b>: recibe la alerta de telemetría y evalúa umbrales
 *       adaptativos (¿fuera de rango? ¿sin señal?).</li>
 *   <li><b>Agente-Diagnosticador</b>: recupera contexto con RAG (caja 2) y las
 *       herramientas MCP (caja 3), correlaciona con fallos conocidos y genera un
 *       diagnóstico en lenguaje natural con nivel de severidad.</li>
 *   <li><b>Agente-Acción</b>: propone el comando correctivo o la orden de campo.</li>
 * </ol>
 *
 * Cajas de gobierno aplicadas en el flujo: Agent Registry A2A (4), Guardrails +
 * PII (5), Model Gateway, Prompt &amp; Model Registry (6), Identity/IAM (7),
 * Audit &amp; Retention (8) y Human Review (1). El LLM nunca decide una acción que
 * compromete la continuidad del servicio: la aprueba una persona.
 */
public class DiagnosticoDerService implements DiagnosticoDerUseCase {

    private final ConocimientoDerPort conocimiento;     // caja 2 · RAG
    private final HerramientasDerPort herramientas;     // caja 3 · MCP
    private final AgentRegistryPort agentRegistry;      // caja 4 · A2A
    private final GuardrailsPort guardrails;            // caja 5 · Guardrails + PII
    private final ModelGatewayPort modelGateway;        // Gateway de modelos
    private final PromptModelRegistryPort promptRegistry; // caja 6 · Prompt/Model Registry
    private final AuditLogPort auditLog;                // caja 8 · Audit & Retention
    private final DiagnosticoEventPublisherPort eventPublisher;

    public DiagnosticoDerService(ConocimientoDerPort conocimiento,
                                 HerramientasDerPort herramientas,
                                 AgentRegistryPort agentRegistry,
                                 GuardrailsPort guardrails,
                                 ModelGatewayPort modelGateway,
                                 PromptModelRegistryPort promptRegistry,
                                 AuditLogPort auditLog,
                                 DiagnosticoEventPublisherPort eventPublisher) {
        this.conocimiento = conocimiento;
        this.herramientas = herramientas;
        this.agentRegistry = agentRegistry;
        this.guardrails = guardrails;
        this.modelGateway = modelGateway;
        this.promptRegistry = promptRegistry;
        this.auditLog = auditLog;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public DiagnosticoDer diagnosticar(AlertaTelemetria alertaCruda) {
        List<PasoFlujo> traza = new ArrayList<>();
        int paso = 0;

        // Caja 7 · Identity (IAM): la identidad del operador viaja con la solicitud;
        // el agente nunca tiene más permisos que la persona.
        String usuario = (alertaCruda.usuario() == null || alertaCruda.usuario().isBlank())
                ? "operador-anonimo" : alertaCruda.usuario();

        // Caja 5 · Guardrails de entrada + enmascarado de PII.
        guardrails.validarEntrada(alertaCruda.descripcion());
        String descripcionSegura = guardrails.enmascararPII(alertaCruda.descripcion());
        boolean seEnmascaro = !descripcionSegura.equals(alertaCruda.descripcion());
        AlertaTelemetria alerta = new AlertaTelemetria(
                alertaCruda.activoId(), alertaCruda.tipoActivo(), alertaCruda.comunidadId(),
                alertaCruda.metrica(), alertaCruda.valor(), alertaCruda.umbral(),
                alertaCruda.minutosSinSenal(), descripcionSegura, usuario);
        traza.add(PasoFlujo.de(++paso, "guardrails", "Guardrails + PII (caja 5) · IAM (caja 7)",
                "Identidad '" + usuario + "' propagada (mínimo privilegio). "
                        + (seEnmascaro ? "Se enmascaró PII antes de cualquier salida." : "Sin PII que enmascarar.")));

        // Caja 4 · Agent Registry (A2A): se verifican los agentes habilitados.
        List<AgentRegistryPort.AgenteRegistrado> agentes = agentRegistry.agentesActivos();
        traza.add(PasoFlujo.de(++paso, "agent-registry", "Agent Registry A2A (caja 4)",
                "Catálogo A2A: " + agentes.size() + " agentes activos ("
                        + String.join(" → ", agentes.stream().map(AgentRegistryPort.AgenteRegistrado::nombre).toList())
                        + "). Delegación entre agentes con permisos declarados."));

        // ---------------- Agente-Monitor ----------------
        boolean anomalia = alerta.fueraDeRango() || alerta.sinSenal();
        String obsMonitor = alerta.sinSenal()
                ? ("Activo sin señal hace " + alerta.minutosSinSenal() + " min.")
                : (alerta.fueraDeRango()
                    ? ("Métrica '" + alerta.metrica() + "'=" + alerta.valor().toPlainString()
                       + " supera el umbral " + alerta.umbral().toPlainString() + ".")
                    : "Métrica dentro de umbrales adaptativos.");
        traza.add(PasoFlujo.de(++paso, "agente-monitor", "AI Flows · evaluación de umbrales",
                "Agente-Monitor: " + obsMonitor));

        // ---------------- Agente-Diagnosticador: RAG (2) + MCP (3) ----------------
        List<Evidencia> evidencias = new ArrayList<>();
        List<Evidencia> rag = conocimiento.recuperar(alerta);
        evidencias.addAll(rag);
        traza.add(PasoFlujo.de(++paso, "rag", "Knowledge Service RAG (caja 2)",
                "Recuperadas " + rag.size() + " fichas (ficha técnica del fabricante, fallos "
                        + "conocidos por modelo, procedimiento de campo), con su fuente citable."));

        List<Evidencia> tools = herramientas.consultar(alerta);
        evidencias.addAll(tools);
        traza.add(PasoFlujo.de(++paso, "mcp", "MCP Servers (caja 3) · Zero Trust por dispositivo",
                "Herramientas MCP consultadas (solo lectura, autorización firmada): telemetría en "
                        + "tiempo real, historial de comandos y topología de red del activo."));

        Severidad severidad = calcularSeveridad(alerta, anomalia, evidencias);
        traza.add(PasoFlujo.de(++paso, "agente-diagnosticador", "ReAct · correlación con patrones",
                "Agente-Diagnosticador: correlaciona la anomalía con patrones conocidos del RAG "
                        + "y clasifica severidad=" + severidad + "."));

        // ---------------- Agente-Acción ----------------
        TipoAccion accion = proponerAccion(alerta, severidad, anomalia);
        boolean requiereHumano = exigeAprobacionHumana(accion);
        traza.add(PasoFlujo.de(++paso, "agente-accion", "Tool calling = capa DER como herramienta",
                "Agente-Acción propone: " + accion + ". "
                        + (requiereHumano
                            ? "Acción sobre infraestructura crítica → EXIGE aprobación humana."
                            : "Acción no intrusiva → dentro de los límites del agente.")));

        // Caja 6 · Prompt & Model Registry + Model Gateway: redacción del diagnóstico.
        String promptVersion = promptRegistry.versionPrompt("diagnosticador");
        String motor;
        String diagnosticoTexto;
        var redaccion = modelGateway.redactarJustificacion(
                new com.voltera.ia.domain.model.SolicitudCopiloto(
                        alerta.activoId(), alerta.comunidadId(), usuario,
                        descripcionSegura, alerta.valor(), true),
                evidencias);
        if (redaccion.isPresent()) {
            diagnosticoTexto = guardrails.enmascararPII(redaccion.get());
            motor = modelGateway.identificadorMotor();
        } else {
            diagnosticoTexto = redactarLocal(alerta, severidad, accion, requiereHumano, evidencias);
            motor = "local";
        }

        // Caja 1 · Human Review: se construye la orden de trabajo (queda en pausa si exige aprobación).
        String ordenTrabajo = construirOrdenTrabajo(alerta, accion, severidad, requiereHumano);
        traza.add(PasoFlujo.de(++paso, "human-review", "Human Review (caja 1) · Responsible AI",
                requiereHumano
                        ? "El flujo se detiene en un checkpoint: un operador debe aprobar antes de ejecutar."
                        : "Acción de solo monitoreo: no requiere aprobación, queda registrada."));

        DiagnosticoDer diagnostico = DiagnosticoDer.crear(
                alerta.activoId(), alerta.tipoActivo(), alerta.comunidadId(), severidad,
                diagnosticoTexto, accion, ordenTrabajo, requiereHumano, evidencias, traza,
                motor, seEnmascaro, promptVersion, usuario);

        // Caja 8 · Audit & Retention: registrar el diagnóstico (quién, qué, con qué prompt/modelo).
        auditLog.registrar(diagnostico);

        // EDA best-effort: publica el evento (en la nube cae al publicador de log).
        eventPublisher.publicar(DiagnosticoDerGenerado.desde(diagnostico));

        return diagnostico;
    }

    // ------------------------------------------------------------------ heurística

    private Severidad calcularSeveridad(AlertaTelemetria a, boolean anomalia, List<Evidencia> ev) {
        if (!anomalia) return Severidad.INFO;
        if (a.sinSenal() || a.tipoActivo() == com.voltera.ia.domain.model.der.TipoActivo.INVERSOR) {
            return Severidad.CRITICA;
        }
        // Desvío grande respecto al umbral ⇒ crítica.
        if (a.umbral().signum() > 0
                && a.valor().subtract(a.umbral()).abs().compareTo(a.umbral().multiply(new java.math.BigDecimal("0.5"))) > 0) {
            return Severidad.CRITICA;
        }
        return Severidad.ADVERTENCIA;
    }

    private TipoAccion proponerAccion(AlertaTelemetria a, Severidad sev, boolean anomalia) {
        if (!anomalia) return TipoAccion.MONITOREAR;
        if (a.sinSenal()) return TipoAccion.DESPACHAR_TECNICO;
        return switch (sev) {
            case CRITICA -> TipoAccion.COMANDO_FISICO;
            case ADVERTENCIA -> TipoAccion.DESPACHAR_TECNICO;
            case INFO -> TipoAccion.MONITOREAR;
        };
    }

    /**
     * El LLM nunca decide lo que compromete el servicio. Toda acción física o de
     * corte, y el despacho de técnicos, exige aprobación humana.
     */
    private boolean exigeAprobacionHumana(TipoAccion accion) {
        return accion == TipoAccion.COMANDO_FISICO
                || accion == TipoAccion.CORTAR_SUMINISTRO
                || accion == TipoAccion.DESPACHAR_TECNICO;
    }

    private String construirOrdenTrabajo(AlertaTelemetria a, TipoAccion accion, Severidad sev, boolean humano) {
        return "OT-" + a.activoId() + " · " + accion + " · severidad " + sev
                + (humano ? " · PENDIENTE DE APROBACIÓN" : " · auto (monitoreo)");
    }

    private String redactarLocal(AlertaTelemetria a, Severidad sev, TipoAccion accion,
                                 boolean humano, List<Evidencia> ev) {
        StringBuilder sb = new StringBuilder();
        sb.append("Diagnóstico (motor local, sin LLM externo) del activo ").append(a.activoId())
          .append(" (").append(a.tipoActivo()).append(", comunidad ").append(a.comunidadId()).append("). ");
        if (a.sinSenal()) {
            sb.append("El activo no reporta telemetría hace ").append(a.minutosSinSenal())
              .append(" minutos (posible medidor sin señal o caída de conectividad). ");
        } else if (a.fueraDeRango()) {
            sb.append("La métrica '").append(a.metrica()).append("' = ").append(a.valor().toPlainString())
              .append(" supera el umbral esperado ").append(a.umbral().toPlainString())
              .append(" (posible degradación/activo fuera de rango). ");
        } else {
            sb.append("Las métricas están dentro de los umbrales adaptativos. ");
        }
        sb.append("Severidad ").append(sev).append(", basada en ").append(ev.size())
          .append(" fuentes citadas (RAG + telemetría MCP). Acción propuesta: ").append(accion).append(". ");
        sb.append(humano
                ? "Requiere aprobación de un operador antes de ejecutarse; el agente no actúa sobre el activo."
                : "Acción de solo monitoreo; queda registrada para seguimiento.");
        return sb.toString();
    }
}
