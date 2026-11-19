package com.voltera.ia.infrastructure.rest;

import com.voltera.ia.domain.model.der.DiagnosticoDer;
import com.voltera.ia.domain.port.in.DiagnosticoDerUseCase;
import com.voltera.ia.domain.port.out.AgentRegistryPort;
import com.voltera.ia.infrastructure.rest.dto.DiagnosticarRequest;
import com.voltera.ia.infrastructure.rest.dto.DiagnosticoResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * API del caso del Grupo 5: Agente de diagnóstico de activos DER.
 *
 * <ul>
 *   <li>POST /api/v1/ia/diagnostico/der — ejecuta el flujo de 3 agentes sobre una
 *       alerta y devuelve el diagnóstico con la acción propuesta.</li>
 *   <li>GET  /api/v1/ia/agentes — catálogo A2A (Agent Registry, caja 4).</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/ia")
public class DiagnosticoDerController {

    private final DiagnosticoDerUseCase diagnostico;
    private final AgentRegistryPort agentRegistry;
    private final com.voltera.ia.infrastructure.revision.RegistroAlertas alertas;
    private final com.voltera.ia.infrastructure.observabilidad.ObservabilidadIa obs;

    public DiagnosticoDerController(DiagnosticoDerUseCase diagnostico, AgentRegistryPort agentRegistry,
                                    com.voltera.ia.infrastructure.revision.RegistroAlertas alertas,
                                    com.voltera.ia.infrastructure.observabilidad.ObservabilidadIa obs) {
        this.diagnostico = diagnostico;
        this.agentRegistry = agentRegistry;
        this.alertas = alertas;
        this.obs = obs;
    }

    @PostMapping("/diagnostico/der")
    public ResponseEntity<DiagnosticoResponse> diagnosticar(@Valid @RequestBody DiagnosticarRequest req) {
        java.time.Instant llegada = java.time.Instant.now();
        var alerta = req.aDominio();
        DiagnosticoDer d = diagnostico.diagnosticar(alerta);
        java.time.Instant observado = req.observadoEn() != null && req.observadoEn().isBefore(llegada) ? req.observadoEn() : llegada;
        alertas.guardar(d.diagnosticoId(), alerta, observado, d.generadoEn());
        obs.registrarTraza(d.traza(), llegada);
        return ResponseEntity.ok(DiagnosticoResponse.desde(d));
    }

    @GetMapping("/agentes")
    public ResponseEntity<List<AgentRegistryPort.AgenteRegistrado>> agentes() {
        return ResponseEntity.ok(agentRegistry.agentesActivos());
    }
}
