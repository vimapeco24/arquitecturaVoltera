package com.voltera.ia.infrastructure.rest;

import com.voltera.ia.domain.model.der.DiagnosticoDer;
import com.voltera.ia.domain.model.gobernanza.Firmante;
import com.voltera.ia.domain.model.gobernanza.PropuestaAccion;
import com.voltera.ia.domain.port.in.GobernanzaAccionUseCase;
import com.voltera.ia.domain.port.out.DiagnosticoRepositoryPort;
import com.voltera.ia.infrastructure.rest.dto.FirmaRequest;
import com.voltera.ia.infrastructure.rest.dto.PropuestaResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * API de la gobernanza multi-firma 3/3 (contrato MultiFirmaAccionDER vía MCP
 * Blockchain Gateway). Materializa el ciclo del documento:
 *
 * <ul>
 *   <li>POST /api/v1/ia/gobernanza/propuestas/{diagnosticoId} — el Agente de
 *       Acción crea la propuesta (Firma 1) a partir de un diagnóstico.</li>
 *   <li>POST .../propuestas/{id}/firmas/operador|prosumidor — firma 2 y 3.</li>
 *   <li>POST .../propuestas/{id}/rechazos/operador|prosumidor — rechazo.</li>
 *   <li>POST .../vencidas — caduca las propuestas cuyo TTL expiró.</li>
 *   <li>GET  .../propuestas[/{id}] — estado del ciclo (0/3 → 3/3).</li>
 * </ul>
 *
 * La firma es un control explícito y separado de la conversación del Assistant.
 */
@RestController
@RequestMapping("/api/v1/ia/gobernanza")
public class GobernanzaController {

    private final GobernanzaAccionUseCase gobernanza;
    private final DiagnosticoRepositoryPort diagnosticos;

    public GobernanzaController(GobernanzaAccionUseCase gobernanza, DiagnosticoRepositoryPort diagnosticos) {
        this.gobernanza = gobernanza;
        this.diagnosticos = diagnosticos;
    }

    @PostMapping("/propuestas/{diagnosticoId}")
    public ResponseEntity<PropuestaResponse> proponer(@PathVariable String diagnosticoId) {
        DiagnosticoDer d = diagnosticos.porId(UUID.fromString(diagnosticoId)).orElseThrow(
                () -> new IllegalArgumentException("Diagnóstico no encontrado: " + diagnosticoId));
        PropuestaAccion p = gobernanza.proponer(d);
        return ResponseEntity.ok(PropuestaResponse.desde(p));
    }

    @PostMapping("/propuestas/{id}/firmas/operador")
    public ResponseEntity<PropuestaResponse> firmarOperador(@PathVariable String id, @Valid @RequestBody FirmaRequest req) {
        return aprobar(id, Firmante.OPERADOR, req);
    }

    @PostMapping("/propuestas/{id}/firmas/prosumidor")
    public ResponseEntity<PropuestaResponse> firmarProsumidor(@PathVariable String id, @Valid @RequestBody FirmaRequest req) {
        return aprobar(id, Firmante.PROSUMIDOR, req);
    }

    @PostMapping("/propuestas/{id}/rechazos/operador")
    public ResponseEntity<PropuestaResponse> rechazarOperador(@PathVariable String id, @Valid @RequestBody FirmaRequest req) {
        return rechazar(id, Firmante.OPERADOR, req);
    }

    @PostMapping("/propuestas/{id}/rechazos/prosumidor")
    public ResponseEntity<PropuestaResponse> rechazarProsumidor(@PathVariable String id, @Valid @RequestBody FirmaRequest req) {
        return rechazar(id, Firmante.PROSUMIDOR, req);
    }

    @PostMapping("/vencidas")
    public ResponseEntity<List<PropuestaResponse>> vencidas() {
        return ResponseEntity.ok(gobernanza.caducarVencidas().stream().map(PropuestaResponse::desde).toList());
    }

    @GetMapping("/propuestas")
    public ResponseEntity<List<PropuestaResponse>> listar() {
        return ResponseEntity.ok(gobernanza.listar().stream().map(PropuestaResponse::desde).toList());
    }

    @GetMapping("/propuestas/{id}")
    public ResponseEntity<PropuestaResponse> consultar(@PathVariable String id) {
        return gobernanza.consultar(UUID.fromString(id))
                .map(p -> ResponseEntity.ok(PropuestaResponse.desde(p)))
                .orElse(ResponseEntity.notFound().build());
    }

    private ResponseEntity<PropuestaResponse> aprobar(String id, Firmante f, FirmaRequest req) {
        PropuestaAccion p = gobernanza.aprobar(UUID.fromString(id), f, req.identidad(), req.razon());
        return ResponseEntity.ok(PropuestaResponse.desde(p));
    }

    private ResponseEntity<PropuestaResponse> rechazar(String id, Firmante f, FirmaRequest req) {
        PropuestaAccion p = gobernanza.rechazar(UUID.fromString(id), f, req.identidad(), req.razon());
        return ResponseEntity.ok(PropuestaResponse.desde(p));
    }
}
