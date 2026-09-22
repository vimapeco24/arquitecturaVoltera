package com.voltera.tarifas.infrastructure.rest;

import com.voltera.tarifas.domain.port.in.ConsultarTarifaUseCase;
import com.voltera.tarifas.domain.port.in.GestionarTarifaUseCase;
import com.voltera.tarifas.infrastructure.rest.dto.CrearTarifaRequest;
import com.voltera.tarifas.infrastructure.rest.dto.TarifaResponse;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Adaptador de ENTRADA REST del servicio de Tarifas.
 */
@RestController
@RequestMapping("/api/v1/tarifas")
public class TarifaController {

    private final GestionarTarifaUseCase gestionarTarifaUseCase;
    private final ConsultarTarifaUseCase consultarTarifaUseCase;

    public TarifaController(GestionarTarifaUseCase gestionarTarifaUseCase,
                            ConsultarTarifaUseCase consultarTarifaUseCase) {
        this.gestionarTarifaUseCase = gestionarTarifaUseCase;
        this.consultarTarifaUseCase = consultarTarifaUseCase;
    }

    @PostMapping
    public ResponseEntity<TarifaResponse> crear(@Valid @RequestBody CrearTarifaRequest req) {
        var franjas = req.franjas().stream()
                .map(f -> new GestionarTarifaUseCase.FranjaDTO(f.horaInicio(), f.horaFin(), f.precioKwh()))
                .toList();
        var tarifa = gestionarTarifaUseCase.crear(
                new GestionarTarifaUseCase.ComandoCrearTarifa(req.nombre(), franjas));
        return ResponseEntity.status(HttpStatus.CREATED).body(TarifaResponse.desde(tarifa));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TarifaResponse> porId(@PathVariable String id) {
        return ResponseEntity.ok(TarifaResponse.desde(consultarTarifaUseCase.porId(id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TarifaResponse> actualizar(@PathVariable String id,
                                                     @Valid @RequestBody CrearTarifaRequest req) {
        var franjas = req.franjas().stream()
                .map(f -> new GestionarTarifaUseCase.FranjaDTO(f.horaInicio(), f.horaFin(), f.precioKwh()))
                .toList();
        var tarifa = gestionarTarifaUseCase.actualizar(id,
                new GestionarTarifaUseCase.ComandoCrearTarifa(req.nombre(), franjas));
        return ResponseEntity.ok(TarifaResponse.desde(tarifa));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable String id) {
        gestionarTarifaUseCase.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<TarifaResponse>> listar() {
        return ResponseEntity.ok(consultarTarifaUseCase.listar().stream()
                .map(TarifaResponse::desde).toList());
    }

    @GetMapping("/{id}/precio")
    public ResponseEntity<Map<String, Object>> precioEn(@PathVariable String id,
                                                        @RequestParam int hora) {
        BigDecimal precio = gestionarTarifaUseCase.precioEn(id, hora);
        return ResponseEntity.ok(Map.of("tarifaId", id, "hora", hora, "precioKwh", precio));
    }
}
