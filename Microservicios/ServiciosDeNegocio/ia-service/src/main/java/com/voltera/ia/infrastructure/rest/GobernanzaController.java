package com.voltera.ia.infrastructure.rest;

import com.voltera.ia.domain.model.der.DiagnosticoDer;
import com.voltera.ia.domain.model.gobernanza.AuditoriaPropuesta;
import com.voltera.ia.domain.model.gobernanza.EstadoAprobaciones;
import com.voltera.ia.domain.model.gobernanza.MetricasGobernanza;
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
 *   <li>GET  .../propuestas/{id}/aprobaciones — firmantes requeridos, quién firmó
 *       (tx + enlace al explorador), quién falta, estado global y modo ON_CHAIN/OFF_CHAIN.</li>
 *   <li>GET  .../propuestas/{id}/auditoria — línea de tiempo de auditoría (CREG).</li>
 *   <li>GET  .../metricas — indicadores agregados (estados, tasas, ejecutadasSin3de3 = 0).</li>
 * </ul>
 *
 * La firma es un control explícito y separado de la conversación del Assistant.
 */
@RestController
@RequestMapping("/api/v1/ia/gobernanza")
public class GobernanzaController {

    private final GobernanzaAccionUseCase gobernanza;
    private final DiagnosticoRepositoryPort diagnosticos;
    private final com.voltera.ia.infrastructure.audit.AuditoriaIa auditoria;

    public GobernanzaController(GobernanzaAccionUseCase gobernanza, DiagnosticoRepositoryPort diagnosticos,
                                com.voltera.ia.infrastructure.audit.AuditoriaIa auditoria) {
        this.gobernanza = gobernanza;
        this.diagnosticos = diagnosticos;
        this.auditoria = auditoria;
    }

    @PostMapping("/propuestas/{diagnosticoId}")
    public ResponseEntity<PropuestaResponse> proponer(@PathVariable String diagnosticoId) {
        DiagnosticoDer d = diagnosticos.porId(UUID.fromString(diagnosticoId)).orElseThrow(
                () -> new IllegalArgumentException("Diagnóstico no encontrado: " + diagnosticoId));
        PropuestaAccion p = gobernanza.proponer(d);
        auditoria.registrar("PROPUESTA", "agente-accion@voltera", p.propuestaId().toString(),
                p.comando() + " · " + p.firmasResumen() + " · hash " + p.hashDiagnostico().substring(0, 18), p.versionPrompt());
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

    @GetMapping("/propuestas/{id}/aprobaciones")
    public ResponseEntity<EstadoAprobaciones> aprobaciones(@PathVariable String id) {
        return gobernanza.aprobaciones(UUID.fromString(id))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /** Línea de tiempo de auditoría (quién, qué, cuándo, tx) de una propuesta. */
    @GetMapping("/propuestas/{id}/auditoria")
    public ResponseEntity<AuditoriaPropuesta> auditoria(@PathVariable String id) {
        return gobernanza.consultar(UUID.fromString(id))
                .map(p -> ResponseEntity.ok(AuditoriaPropuesta.desde(p)))
                .orElse(ResponseEntity.notFound().build());
    }

    /** Indicadores agregados de la gobernanza (volumen por estado, tasas, garantía fail-closed). */
    @GetMapping("/metricas")
    public ResponseEntity<MetricasGobernanza> metricas() {
        return ResponseEntity.ok(MetricasGobernanza.desde(gobernanza.listar()));
    }

    /** Mensaje canónico a firmar con la billetera (personal_sign) para aprobar o rechazar. */
    @GetMapping("/propuestas/{id}/mensaje-firma")
    public ResponseEntity<java.util.Map<String, String>> mensajeFirma(@PathVariable String id,
            @org.springframework.web.bind.annotation.RequestParam String rol,
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "APRUEBA") String decision) {
        Firmante f = Firmante.valueOf(rol.toUpperCase());
        return gobernanza.mensajeFirma(UUID.fromString(id), f, !"RECHAZA".equalsIgnoreCase(decision))
                .map(m -> ResponseEntity.ok(java.util.Map.of("mensaje", m, "rol", f.name(), "estandar", "EIP-191 personal_sign")))
                .orElse(ResponseEntity.notFound().build());
    }

    /** Verificación criptográfica (ecrecover) de las firmas de la propuesta. */
    @GetMapping("/propuestas/{id}/firmas/verificacion")
    public ResponseEntity<List<com.voltera.ia.domain.model.gobernanza.VerificacionFirma>> verificacion(@PathVariable String id) {
        return gobernanza.verificarFirmas(UUID.fromString(id))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    private ResponseEntity<PropuestaResponse> aprobar(String id, Firmante f, FirmaRequest req) {
        com.voltera.ia.infrastructure.security.IamContexto.exigirRol(f.name());
        String quien = com.voltera.ia.infrastructure.security.IamContexto.identidadO(req.identidad());
        PropuestaAccion p = gobernanza.aprobar(UUID.fromString(id), f, quien, req.razon(),
                req.direccion(), req.firma());
        auditoria.registrar("FIRMA", quien, id, f + " aprueba · " + p.firmasResumen() + " · " + p.estado()
                + (req.firma() != null && !req.firma().isBlank() ? " · billetera" : " · clave custodiada"), p.versionPrompt());
        if (p.estado() == com.voltera.ia.domain.model.gobernanza.EstadoPropuesta.EJECUTADA) {
            auditoria.registrar("EJECUCION", "agente-accion@voltera", id, p.comando() + " ejecutado con 3/3", p.versionPrompt());
        }
        return ResponseEntity.ok(PropuestaResponse.desde(p));
    }

    private ResponseEntity<PropuestaResponse> rechazar(String id, Firmante f, FirmaRequest req) {
        com.voltera.ia.infrastructure.security.IamContexto.exigirRol(f.name());
        String quien = com.voltera.ia.infrastructure.security.IamContexto.identidadO(req.identidad());
        PropuestaAccion p = gobernanza.rechazar(UUID.fromString(id), f, quien, req.razon(),
                req.direccion(), req.firma());
        auditoria.registrar("RECHAZO", quien, id, f + " rechaza · " + p.estado(), p.versionPrompt());
        return ResponseEntity.ok(PropuestaResponse.desde(p));
    }
}
