package com.voltera.ia.infrastructure.rest;

import com.voltera.ia.domain.model.RecomendacionCopiloto;
import com.voltera.ia.domain.port.in.CopilotoSiniestrosUseCase;
import com.voltera.ia.domain.port.out.ModelGatewayPort;
import com.voltera.ia.infrastructure.rest.dto.AnalizarSiniestroRequest;
import com.voltera.ia.infrastructure.rest.dto.RecomendacionResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * API del Copiloto de Siniestros (plataforma GenAI).
 *
 * <ul>
 *   <li>POST /api/v1/ia/copilotos/siniestros/analizar — ejecuta el flujo
 *       agentico sobre un caso y devuelve una recomendacion (propuesta).</li>
 *   <li>GET  /api/v1/ia/estado — estado de la plataforma: proveedor de modelo
 *       activo o motor local (para mostrarlo en la consola).</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/ia")
public class CopilotoController {

    private final CopilotoSiniestrosUseCase copiloto;
    private final ModelGatewayPort modelGateway;

    public CopilotoController(CopilotoSiniestrosUseCase copiloto, ModelGatewayPort modelGateway) {
        this.copiloto = copiloto;
        this.modelGateway = modelGateway;
    }

    @PostMapping("/copilotos/siniestros/analizar")
    public ResponseEntity<RecomendacionResponse> analizar(@Valid @RequestBody AnalizarSiniestroRequest req) {
        RecomendacionCopiloto recomendacion = copiloto.analizar(req.aDominio());
        return ResponseEntity.ok(RecomendacionResponse.desde(recomendacion));
    }

    @GetMapping("/estado")
    public ResponseEntity<Map<String, Object>> estado() {
        return ResponseEntity.ok(Map.of(
                "servicio", "ia-service",
                "caso", "Agente de diagnóstico de activos DER y copiloto de soporte de red (Grupo 5)",
                "agentes", java.util.List.of("Agente-Monitor", "Agente-Diagnosticador", "Agente-Accion"),
                "cajas", java.util.List.of(
                        "1-Human Review", "2-Knowledge Service (RAG)", "3-MCP Servers",
                        "4-Agent Registry (A2A)", "5-Guardrails", "6-Prompt & Model Registry",
                        "7-Identity (IAM)", "8-Audit & Retention"),
                "proveedorModeloActivo", modelGateway.proveedorActivo(),
                "motor", modelGateway.identificadorMotor(),
                "zeroRetention", true,
                "humanInTheLoop", true));
    }
}
