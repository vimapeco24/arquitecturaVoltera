package com.voltera.certificados.infrastructure.rest;

import com.voltera.certificados.domain.port.in.GestionarCertificadosUseCase;
import com.voltera.certificados.domain.port.in.GestionarCertificadosUseCase.ComandoEmitir;
import com.voltera.certificados.domain.port.out.GatewayCadenaPort;
import com.voltera.certificados.infrastructure.rest.dto.CertificadoResponse;
import com.voltera.certificados.infrastructure.rest.dto.EmitirRequest;
import com.voltera.certificados.infrastructure.rest.dto.TransferirRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * API REST del Servicio de Certificados Renovables (lámina 04). NO toca la cadena
 * directamente: usa el gateway. Ruta base: /certificados-renovables/v1/api/v1/...
 */
@RestController
@RequestMapping("/api/v1/certificados-renovables")
public class CertificadosController {

    private final GestionarCertificadosUseCase uc;
    private final GatewayCadenaPort gateway;

    public CertificadosController(GestionarCertificadosUseCase uc, GatewayCadenaPort gateway) {
        this.uc = uc;
        this.gateway = gateway;
    }

    @PostMapping
    public ResponseEntity<CertificadoResponse> emitir(@Valid @RequestBody EmitirRequest req) {
        var c = uc.emitir(new ComandoEmitir(
                req.idMedidor(), req.emisor(), req.titular(),
                req.consumoNetoMensualWh(), req.periodo(), req.solicitudCertificacionId()));
        return ResponseEntity.status(HttpStatus.CREATED).body(CertificadoResponse.desde(c));
    }

    @PostMapping("/{id}/transferir")
    public CertificadoResponse transferir(@PathVariable String id, @Valid @RequestBody TransferirRequest req) {
        return CertificadoResponse.desde(uc.transferir(id, req.hacia()));
    }

    @PostMapping("/{id}/retirar")
    public CertificadoResponse retirar(@PathVariable String id) {
        return CertificadoResponse.desde(uc.retirar(id));
    }

    @GetMapping("/{id}")
    public CertificadoResponse porId(@PathVariable String id) {
        return CertificadoResponse.desde(uc.porId(id));
    }

    @GetMapping
    public List<CertificadoResponse> todos(@RequestParam(required = false) String titular) {
        var lista = (titular == null || titular.isBlank()) ? uc.todos() : uc.porTitular(titular);
        return lista.stream().map(CertificadoResponse::desde).toList();
    }

    /** Estado de la integración con el gateway (para el badge del front). */
    @GetMapping("/gateway")
    public Map<String, Object> gateway() {
        return Map.of(
                "tocaLaCadena", false,
                "usaGateway", true,
                "gatewayConfigurado", gateway.configurado(),
                "modo", gateway.configurado() ? "REST al gateway" : "demo (gateway simulado)");
    }
}
