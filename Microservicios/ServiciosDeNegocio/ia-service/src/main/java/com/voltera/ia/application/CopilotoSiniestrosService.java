package com.voltera.ia.application;

import com.voltera.ia.domain.model.Decision;
import com.voltera.ia.domain.model.Evidencia;
import com.voltera.ia.domain.model.IaRecomendacionGenerada;
import com.voltera.ia.domain.model.NivelConfianza;
import com.voltera.ia.domain.model.PasoFlujo;
import com.voltera.ia.domain.model.RecomendacionCopiloto;
import com.voltera.ia.domain.model.SolicitudCopiloto;
import com.voltera.ia.domain.port.in.CopilotoSiniestrosUseCase;
import com.voltera.ia.domain.port.out.ConocimientoPort;
import com.voltera.ia.domain.port.out.EventPublisherPort;
import com.voltera.ia.domain.port.out.GuardrailsPort;
import com.voltera.ia.domain.port.out.HerramientasPort;
import com.voltera.ia.domain.port.out.ModelGatewayPort;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Servicio de aplicacion: el Copiloto de Siniestros como flujo agentico acotado
 * (grafo tipo LangGraph). Reproduce el caso integrador del modulo GenAI, pero no
 * inventa infraestructura: reutiliza las piezas de los modulos anteriores.
 *
 * <p>Flujo (7 nodos, cada uno deja traza observable):</p>
 * <ol>
 *   <li><b>disparo</b> — evento SiniestroReportado/Aprobado o consulta manual.</li>
 *   <li><b>RAG</b> — recupera poliza, cobertura, normativa, historial (citado).</li>
 *   <li><b>ReAct</b> — razona que falta y que herramienta usar.</li>
 *   <li><b>tool-calling</b> — llama microservicios como herramientas (MCP/REST).</li>
 *   <li><b>cadena</b> — consulta el riesgo cedido a reaseguradoras.</li>
 *   <li><b>human-in-the-loop</b> — por monto/confianza decide si exige revision.</li>
 *   <li><b>registro</b> — redacta (Model Gateway con guardrails), publica evento.</li>
 * </ol>
 *
 * <p>El agente PROPONE; la persona DECIDE. Si no hay proveedor de LLM, la
 * justificacion la genera el motor local (Zero-retention por defecto).</p>
 */
public class CopilotoSiniestrosService implements CopilotoSiniestrosUseCase {

    private final ConocimientoPort conocimiento;
    private final HerramientasPort herramientas;
    private final ModelGatewayPort modelGateway;
    private final GuardrailsPort guardrails;
    private final EventPublisherPort eventPublisher;
    private final BigDecimal umbralRevisionHumana;

    public CopilotoSiniestrosService(ConocimientoPort conocimiento,
                                     HerramientasPort herramientas,
                                     ModelGatewayPort modelGateway,
                                     GuardrailsPort guardrails,
                                     EventPublisherPort eventPublisher,
                                     BigDecimal umbralRevisionHumana) {
        this.conocimiento = conocimiento;
        this.herramientas = herramientas;
        this.modelGateway = modelGateway;
        this.guardrails = guardrails;
        this.eventPublisher = eventPublisher;
        this.umbralRevisionHumana = umbralRevisionHumana;
    }

    @Override
    public RecomendacionCopiloto analizar(SolicitudCopiloto solicitudCruda) {
        List<PasoFlujo> traza = new ArrayList<>();
        int paso = 0;

        // --- Nodo 1: disparo ------------------------------------------------
        String origen = solicitudCruda.disparadoPorEvento()
                ? "evento del broker (SiniestroReportado/Aprobado)"
                : "consulta manual del analista";
        traza.add(PasoFlujo.de(++paso, "disparo", "Disparo por evento",
                "El copiloto despierta por " + origen + " para el siniestro "
                        + nz(solicitudCruda.siniestroId()) + "."));

        // --- Guardrail de entrada + enmascarado de PII ----------------------
        guardrails.validarEntrada(solicitudCruda.descripcion());
        String descripcionSegura = guardrails.enmascararPII(solicitudCruda.descripcion());
        boolean seEnmascaro = !descripcionSegura.equals(solicitudCruda.descripcion());
        SolicitudCopiloto solicitud = new SolicitudCopiloto(
                solicitudCruda.siniestroId(), solicitudCruda.polizaId(),
                solicitudCruda.prosumidorId(), descripcionSegura,
                solicitudCruda.montoReclamacion(), solicitudCruda.disparadoPorEvento());
        traza.add(PasoFlujo.de(++paso, "guardrails", "Responsible AI",
                seEnmascaro
                        ? "Guardrail de entrada OK; se enmascaro PII antes de cualquier salida de datos."
                        : "Guardrail de entrada OK; sin PII que enmascarar."));

        List<Evidencia> evidencias = new ArrayList<>();

        // --- Nodo 2: RAG ----------------------------------------------------
        List<Evidencia> rag = conocimiento.recuperar(solicitud);
        evidencias.addAll(rag);
        traza.add(PasoFlujo.de(++paso, "rag", "RAG (recuperacion aumentada)",
                "Recuperadas " + rag.size() + " fichas de conocimiento (poliza, cobertura, "
                        + "normativa, historial), cada una con su fuente citable."));

        // --- Nodo 3: ReAct --------------------------------------------------
        traza.add(PasoFlujo.de(++paso, "react", "ReAct (razonar-actuar-observar)",
                "El agente razona sobre lo recuperado: identifica que debe validar el "
                        + "estado del siniestro y el riesgo cedido antes de proponer."));

        // --- Nodo 4: tool-calling (microservicios como herramientas) --------
        // --- Nodo 5: cadena (riesgo cedido en el consorcio) -----------------
        List<Evidencia> tools = herramientas.consultar(solicitud);
        evidencias.addAll(tools);
        long deCadena = tools.stream().filter(e -> e.fuente().startsWith("chain:")).count();
        long deMsvc = tools.size() - deCadena;
        traza.add(PasoFlujo.de(++paso, "tool-calling", "Tool calling = microservicios herramienta",
                "Herramientas MCP/REST consultadas: " + deMsvc + " microservicio(s). "
                        + "El copiloto lee; no tiene la herramienta para aprobar pagos."));
        traza.add(PasoFlujo.de(++paso, "cadena", "Cadena como fuente de verdad",
                deCadena > 0
                        ? "Consultada la cadena del consorcio: riesgo cedido a reaseguradoras verificado."
                        : "Cadena no disponible/omitida; la propuesta lo advierte."));

        // --- Nodo 6: human-in-the-loop --------------------------------------
        BigDecimal monto = solicitud.montoReclamacion();
        NivelConfianza confianza = calcularConfianza(evidencias);
        boolean superaUmbral = monto.compareTo(umbralRevisionHumana) > 0;
        boolean requiereHumano = superaUmbral || confianza == NivelConfianza.BAJA;
        traza.add(PasoFlujo.de(++paso, "human-in-the-loop", "Responsible AI · humano en el lazo",
                requiereHumano
                        ? ("Monto " + monto.toPlainString() + " supera el umbral o confianza baja: "
                           + "la propuesta EXIGE revision y aprobacion humana.")
                        : ("Caso dentro de los limites del agente (monto <= umbral y confianza suficiente): "
                           + "propuesta lista para validacion rapida.")));

        Decision decision = proponerDecision(confianza, requiereHumano, evidencias);
        BigDecimal montoSugerido = decision == Decision.APROBAR ? monto : BigDecimal.ZERO;

        // --- Redaccion de la justificacion (Model Gateway + guardrails) -----
        String motor;
        String justificacion;
        Optional<String> redaccionLlm = modelGateway.redactarJustificacion(solicitud, evidencias);
        if (redaccionLlm.isPresent()) {
            justificacion = guardrails.enmascararPII(redaccionLlm.get());
            motor = modelGateway.identificadorMotor();
        } else {
            justificacion = redactarLocal(solicitud, decision, confianza, requiereHumano, evidencias);
            motor = "local";
        }

        // --- Nodo 7: registro + evento --------------------------------------
        traza.add(PasoFlujo.de(++paso, "registro", "Observabilidad y registro",
                "Recomendacion generada con motor '" + motor + "'. Se registra la decision "
                        + "propuesta, su evidencia y se publica el evento topic.ia.recomendacion."));

        RecomendacionCopiloto recomendacion = RecomendacionCopiloto.crear(
                nz(solicitud.siniestroId()), decision, montoSugerido, confianza,
                requiereHumano, justificacion, evidencias, traza, motor, seEnmascaro);

        // EDA best-effort: no romper si el broker esta caido.
        eventPublisher.publicar(IaRecomendacionGenerada.desde(recomendacion));

        return recomendacion;
    }

    // ------------------------------------------------------------------ helpers

    private NivelConfianza calcularConfianza(List<Evidencia> evidencias) {
        boolean hayRag = evidencias.stream().anyMatch(e -> e.fuente().startsWith("RAG:"));
        boolean hayTool = evidencias.stream().anyMatch(e -> e.fuente().startsWith("tool:"));
        boolean hayCadena = evidencias.stream().anyMatch(e -> e.fuente().startsWith("chain:"));
        if (hayRag && hayTool && hayCadena) return NivelConfianza.ALTA;
        if (hayRag && hayTool) return NivelConfianza.MEDIA;
        return NivelConfianza.BAJA;
    }

    private Decision proponerDecision(NivelConfianza confianza, boolean requiereHumano,
                                      List<Evidencia> evidencias) {
        boolean sinEvidenciaSuficiente = evidencias.stream()
                .noneMatch(e -> e.fuente().startsWith("tool:"));
        if (sinEvidenciaSuficiente) return Decision.SOLICITAR_INFO;
        if (requiereHumano) return Decision.ESCALAR_HUMANO;
        return confianza == NivelConfianza.ALTA ? Decision.APROBAR : Decision.SOLICITAR_INFO;
    }

    /**
     * Motor local heuristico y determinista: redacta la justificacion a partir de
     * la evidencia, sin llamar a ningun LLM externo. Permite que el copiloto
     * funcione en demo sin credenciales y sin sacar datos del clúster.
     */
    private String redactarLocal(SolicitudCopiloto s, Decision decision, NivelConfianza confianza,
                                 boolean requiereHumano, List<Evidencia> evidencias) {
        StringBuilder sb = new StringBuilder();
        sb.append("Copiloto de Siniestros (motor local, sin LLM externo). ");
        sb.append("Para el siniestro ").append(nz(s.siniestroId()))
          .append(" de la poliza ").append(nz(s.polizaId())).append(": ");
        switch (decision) {
            case APROBAR -> sb.append("la evidencia recuperada respalda APROBAR la reclamacion por ")
                    .append(s.montoReclamacion().toPlainString()).append(" COP. ");
            case ESCALAR_HUMANO -> sb.append("se recomienda ESCALAR a revision humana antes de cualquier pago. ");
            case RECHAZAR -> sb.append("la evidencia sugiere RECHAZAR la reclamacion. ");
            case SOLICITAR_INFO -> sb.append("falta evidencia verificable; se recomienda SOLICITAR INFORMACION adicional. ");
        }
        sb.append("Confianza ").append(confianza.name().toLowerCase()).append(", basada en ")
          .append(evidencias.size()).append(" fuentes citadas. ");
        if (requiereHumano) {
            sb.append("El monto o el nivel de confianza exigen que un analista valide y apruebe; ")
              .append("el agente no ejecuta el pago (human-in-the-loop).");
        } else {
            sb.append("Caso dentro de los limites del agente; aun asi, la decision final es del analista.");
        }
        return sb.toString();
    }

    private static String nz(String v) {
        return (v == null || v.isBlank()) ? "(sin-id)" : v;
    }
}
