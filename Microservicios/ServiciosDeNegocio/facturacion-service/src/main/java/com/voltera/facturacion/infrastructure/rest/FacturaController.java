package com.voltera.facturacion.infrastructure.rest;

import com.voltera.facturacion.domain.port.in.ConsultarFacturaUseCase;
import com.voltera.facturacion.domain.port.in.EmitirFacturaUseCase;
import com.voltera.facturacion.infrastructure.rest.dto.EmitirFacturaRequest;
import com.voltera.facturacion.infrastructure.rest.dto.FacturaResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Adaptador de ENTRADA (driving adapter): expone los casos de uso via HTTP/REST.
 * Depende de los puertos de entrada, no de la implementacion concreta.
 */
@RestController
@RequestMapping("/api/v1/facturas")
public class FacturaController {

    private final EmitirFacturaUseCase emitirFacturaUseCase;
    private final ConsultarFacturaUseCase consultarFacturaUseCase;

    public FacturaController(EmitirFacturaUseCase emitirFacturaUseCase,
                             ConsultarFacturaUseCase consultarFacturaUseCase) {
        this.emitirFacturaUseCase = emitirFacturaUseCase;
        this.consultarFacturaUseCase = consultarFacturaUseCase;
    }

    @PostMapping
    public ResponseEntity<FacturaResponse> emitir(@Valid @RequestBody EmitirFacturaRequest req) {
        var comando = new EmitirFacturaUseCase.ComandoEmitirFactura(
                req.prosumidorId(),
                req.periodoInicio(),
                req.periodoFin(),
                req.kwhConsumidos(),
                req.kwhInyectados(),
                req.precioConsumoKwh(),
                req.precioExcedenteKwh(),
                req.tarifaId(),
                req.hora()
        );
        var factura = emitirFacturaUseCase.emitir(comando);
        return ResponseEntity.status(HttpStatus.CREATED).body(FacturaResponse.desde(factura));
    }

    @GetMapping("/{id}")
    public ResponseEntity<FacturaResponse> porId(@PathVariable String id) {
        var factura = consultarFacturaUseCase.porId(id);
        return ResponseEntity.ok(FacturaResponse.desde(factura));
    }

    @PutMapping("/{id}")
    public ResponseEntity<FacturaResponse> actualizar(@PathVariable String id,
                                                      @Valid @RequestBody EmitirFacturaRequest req) {
        var comando = new EmitirFacturaUseCase.ComandoEmitirFactura(
                req.prosumidorId(),
                req.periodoInicio(),
                req.periodoFin(),
                req.kwhConsumidos(),
                req.kwhInyectados(),
                req.precioConsumoKwh(),
                req.precioExcedenteKwh(),
                req.tarifaId(),
                req.hora()
        );
        var factura = emitirFacturaUseCase.actualizar(id, comando);
        return ResponseEntity.ok(FacturaResponse.desde(factura));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable String id) {
        emitirFacturaUseCase.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<FacturaResponse>> porProsumidor(
            @org.springframework.web.bind.annotation.RequestParam String prosumidorId) {
        var facturas = consultarFacturaUseCase.porProsumidor(prosumidorId).stream()
                .map(FacturaResponse::desde)
                .toList();
        return ResponseEntity.ok(facturas);
    }
}
