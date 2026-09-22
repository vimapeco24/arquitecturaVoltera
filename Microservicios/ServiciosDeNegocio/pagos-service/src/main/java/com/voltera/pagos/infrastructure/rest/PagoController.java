package com.voltera.pagos.infrastructure.rest;

import com.voltera.pagos.domain.port.in.ConsultarPagoUseCase;
import com.voltera.pagos.domain.port.in.ProcesarPagoUseCase;
import com.voltera.pagos.infrastructure.rest.dto.PagoResponse;
import com.voltera.pagos.infrastructure.rest.dto.ProcesarPagoRequest;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Adaptador de ENTRADA REST del servicio de Pagos.
 */
@RestController
@RequestMapping("/api/v1/pagos")
public class PagoController {

    private final ProcesarPagoUseCase procesarPagoUseCase;
    private final ConsultarPagoUseCase consultarPagoUseCase;

    public PagoController(ProcesarPagoUseCase procesarPagoUseCase,
                          ConsultarPagoUseCase consultarPagoUseCase) {
        this.procesarPagoUseCase = procesarPagoUseCase;
        this.consultarPagoUseCase = consultarPagoUseCase;
    }

    @PostMapping
    public ResponseEntity<PagoResponse> procesar(@Valid @RequestBody ProcesarPagoRequest req) {
        var comando = new ProcesarPagoUseCase.ComandoProcesarPago(
                req.prosumidorId(), req.referencia(), req.tipo(), req.monto());
        var orden = procesarPagoUseCase.procesar(comando);
        return ResponseEntity.status(HttpStatus.CREATED).body(PagoResponse.desde(orden));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PagoResponse> porId(@PathVariable String id) {
        return ResponseEntity.ok(PagoResponse.desde(consultarPagoUseCase.porId(id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PagoResponse> actualizar(@PathVariable String id,
                                                   @Valid @RequestBody ProcesarPagoRequest req) {
        var comando = new ProcesarPagoUseCase.ComandoProcesarPago(
                req.prosumidorId(), req.referencia(), req.tipo(), req.monto());
        var orden = procesarPagoUseCase.actualizar(id, comando);
        return ResponseEntity.ok(PagoResponse.desde(orden));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable String id) {
        procesarPagoUseCase.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<PagoResponse>> porProsumidor(@RequestParam String prosumidorId) {
        return ResponseEntity.ok(consultarPagoUseCase.porProsumidor(prosumidorId).stream()
                .map(PagoResponse::desde).toList());
    }
}
