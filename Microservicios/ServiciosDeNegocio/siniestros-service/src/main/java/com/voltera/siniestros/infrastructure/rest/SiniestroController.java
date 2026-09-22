package com.voltera.siniestros.infrastructure.rest;

import com.voltera.siniestros.domain.port.in.AprobarSiniestroUseCase;
import com.voltera.siniestros.domain.port.in.ConsultarSiniestroUseCase;
import com.voltera.siniestros.domain.port.in.ReportarSiniestroUseCase;
import com.voltera.siniestros.infrastructure.rest.dto.ReportarSiniestroRequest;
import com.voltera.siniestros.infrastructure.rest.dto.SiniestroResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Adaptador de ENTRADA REST del servicio de Siniestros.
 */
@RestController
@RequestMapping("/api/v1/siniestros")
public class SiniestroController {

    private final ReportarSiniestroUseCase reportarSiniestroUseCase;
    private final AprobarSiniestroUseCase aprobarSiniestroUseCase;
    private final ConsultarSiniestroUseCase consultarSiniestroUseCase;

    public SiniestroController(ReportarSiniestroUseCase reportarSiniestroUseCase,
                               AprobarSiniestroUseCase aprobarSiniestroUseCase,
                               ConsultarSiniestroUseCase consultarSiniestroUseCase) {
        this.reportarSiniestroUseCase = reportarSiniestroUseCase;
        this.aprobarSiniestroUseCase = aprobarSiniestroUseCase;
        this.consultarSiniestroUseCase = consultarSiniestroUseCase;
    }

    @PostMapping
    public ResponseEntity<SiniestroResponse> reportar(@Valid @RequestBody ReportarSiniestroRequest req) {
        var comando = new ReportarSiniestroUseCase.ComandoReportarSiniestro(
                req.polizaId(), req.prosumidorId(), req.descripcion(),
                req.montoReclamacion(), req.fechaOcurrencia());
        var siniestro = reportarSiniestroUseCase.reportar(comando);
        return ResponseEntity.status(HttpStatus.CREATED).body(SiniestroResponse.desde(siniestro));
    }

    @PostMapping("/{id}/peritaje")
    public ResponseEntity<SiniestroResponse> enviarAPeritaje(@PathVariable String id) {
        return ResponseEntity.ok(SiniestroResponse.desde(aprobarSiniestroUseCase.enviarAPeritaje(id)));
    }

    @PostMapping("/{id}/aprobar")
    public ResponseEntity<SiniestroResponse> aprobar(@PathVariable String id) {
        return ResponseEntity.ok(SiniestroResponse.desde(aprobarSiniestroUseCase.aprobar(id)));
    }

    @PostMapping("/{id}/rechazar")
    public ResponseEntity<SiniestroResponse> rechazar(@PathVariable String id) {
        return ResponseEntity.ok(SiniestroResponse.desde(aprobarSiniestroUseCase.rechazar(id)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SiniestroResponse> porId(@PathVariable String id) {
        return ResponseEntity.ok(SiniestroResponse.desde(consultarSiniestroUseCase.porId(id)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable String id) {
        reportarSiniestroUseCase.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<SiniestroResponse>> listar(
            @RequestParam(name = "polizaId", required = false) String polizaId) {
        List<SiniestroResponse> resultado = (polizaId != null && !polizaId.isBlank()
                ? consultarSiniestroUseCase.porPoliza(polizaId)
                : consultarSiniestroUseCase.todos())
                .stream().map(SiniestroResponse::desde).toList();
        return ResponseEntity.ok(resultado);
    }
}
