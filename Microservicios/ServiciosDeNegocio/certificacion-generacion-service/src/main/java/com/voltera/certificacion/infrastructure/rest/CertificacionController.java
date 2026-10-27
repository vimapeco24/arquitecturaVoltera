package com.voltera.certificacion.infrastructure.rest;

import com.voltera.certificacion.domain.port.in.CertificarGeneracionUseCase;
import com.voltera.certificacion.domain.port.in.CertificarGeneracionUseCase.ComandoRegistrarGeneracion;
import com.voltera.certificacion.domain.port.in.CertificarGeneracionUseCase.ComandoSolicitar;
import com.voltera.certificacion.infrastructure.rest.dto.BalanceResponse;
import com.voltera.certificacion.infrastructure.rest.dto.RegistrarGeneracionRequest;
import com.voltera.certificacion.infrastructure.rest.dto.SolicitarRequest;
import com.voltera.certificacion.infrastructure.rest.dto.SolicitudResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * API REST del Servicio de Certificación de Generación (lámina 04). NO toca la
 * cadena. Ruta base: /certificacion-generacion/v1/api/v1/...
 */
@RestController
@RequestMapping("/api/v1")
public class CertificacionController {

    private final CertificarGeneracionUseCase uc;

    public CertificacionController(CertificarGeneracionUseCase uc) {
        this.uc = uc;
    }

    @PostMapping("/generacion")
    public ResponseEntity<BalanceResponse> registrarGeneracion(@Valid @RequestBody RegistrarGeneracionRequest req) {
        var balance = uc.registrarGeneracion(new ComandoRegistrarGeneracion(req.medidorId(), req.medicionId(), req.wh()));
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(BalanceResponse.desde(balance));
    }

    @GetMapping("/balances")
    public List<BalanceResponse> balances() {
        return uc.balances().stream().map(BalanceResponse::desde).toList();
    }

    @GetMapping("/balances/{medidorId}")
    public BalanceResponse balance(@PathVariable String medidorId) {
        return BalanceResponse.desde(uc.balanceDe(medidorId));
    }

    @PostMapping("/solicitudes")
    public ResponseEntity<SolicitudResponse> solicitar(@Valid @RequestBody SolicitarRequest req) {
        var s = uc.solicitarCertificacion(new ComandoSolicitar(req.medidorId(), req.propietario(), req.periodo()));
        return ResponseEntity.status(HttpStatus.CREATED).body(SolicitudResponse.desde(s));
    }

    @PostMapping("/solicitudes/{id}/aval-voltera")
    public SolicitudResponse avalarVoltera(@PathVariable String id) {
        return SolicitudResponse.desde(uc.avalarVoltera(id));
    }

    @PostMapping("/solicitudes/{id}/aval-certificadora")
    public SolicitudResponse avalarCertificadora(@PathVariable String id) {
        return SolicitudResponse.desde(uc.avalarCertificadora(id));
    }

    @PostMapping("/solicitudes/{id}/rechazar")
    public SolicitudResponse rechazar(@PathVariable String id, @RequestBody(required = false) Map<String, String> body) {
        String motivo = body == null ? null : body.get("motivo");
        return SolicitudResponse.desde(uc.rechazar(id, motivo));
    }

    @GetMapping("/solicitudes/{id}")
    public SolicitudResponse porId(@PathVariable String id) {
        return SolicitudResponse.desde(uc.porId(id));
    }

    @GetMapping("/solicitudes")
    public List<SolicitudResponse> todas() {
        return uc.todas().stream().map(SolicitudResponse::desde).toList();
    }
}
