package com.voltera.liquidacionmensual.infrastructure.rest;

import com.voltera.liquidacionmensual.domain.port.in.ConsultarLiquidacionUseCase;
import com.voltera.liquidacionmensual.domain.port.in.GestionarLiquidacionUseCase;
import com.voltera.liquidacionmensual.infrastructure.rest.dto.AbrirLiquidacionRequest;
import com.voltera.liquidacionmensual.infrastructure.rest.dto.LiquidacionResponse;
import com.voltera.liquidacionmensual.infrastructure.rest.dto.MovimientoRequest;
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
 * Adaptador de ENTRADA REST del servicio de Liquidacion Mensual.
 * Flujo: abrir -> registrar movimientos (consumo/excedente) -> cerrar (totaliza).
 */
@RestController
@RequestMapping("/api/v1/liquidaciones")
public class LiquidacionMensualController {

    private final GestionarLiquidacionUseCase gestionarUseCase;
    private final ConsultarLiquidacionUseCase consultarUseCase;

    public LiquidacionMensualController(GestionarLiquidacionUseCase gestionarUseCase,
                                        ConsultarLiquidacionUseCase consultarUseCase) {
        this.gestionarUseCase = gestionarUseCase;
        this.consultarUseCase = consultarUseCase;
    }

    @PostMapping
    public ResponseEntity<LiquidacionResponse> abrir(@Valid @RequestBody AbrirLiquidacionRequest req) {
        var comando = new GestionarLiquidacionUseCase.ComandoAbrir(
                req.prosumidorId(), req.anio(), req.mes(),
                req.precioConsumoKwh(), req.precioExcedenteKwh());
        var liquidacion = gestionarUseCase.abrir(comando);
        return ResponseEntity.status(HttpStatus.CREATED).body(LiquidacionResponse.desde(liquidacion));
    }

    @PostMapping("/{id}/movimientos")
    public ResponseEntity<LiquidacionResponse> registrarMovimiento(@PathVariable String id,
                                                                   @Valid @RequestBody MovimientoRequest req) {
        var comando = new GestionarLiquidacionUseCase.ComandoMovimiento(id, req.tipo(), req.kwh());
        var liquidacion = gestionarUseCase.registrarMovimiento(comando);
        return ResponseEntity.ok(LiquidacionResponse.desde(liquidacion));
    }

    @PostMapping("/{id}/cerrar")
    public ResponseEntity<LiquidacionResponse> cerrar(@PathVariable String id) {
        return ResponseEntity.ok(LiquidacionResponse.desde(gestionarUseCase.cerrar(id)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<LiquidacionResponse> porId(@PathVariable String id) {
        return ResponseEntity.ok(LiquidacionResponse.desde(consultarUseCase.porId(id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<LiquidacionResponse> actualizar(@PathVariable String id,
                                                          @Valid @RequestBody AbrirLiquidacionRequest req) {
        var comando = new GestionarLiquidacionUseCase.ComandoAbrir(
                req.prosumidorId(), req.anio(), req.mes(),
                req.precioConsumoKwh(), req.precioExcedenteKwh());
        var liquidacion = gestionarUseCase.actualizar(id, comando);
        return ResponseEntity.ok(LiquidacionResponse.desde(liquidacion));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable String id) {
        gestionarUseCase.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<LiquidacionResponse>> porProsumidor(@RequestParam String prosumidorId) {
        return ResponseEntity.ok(consultarUseCase.porProsumidor(prosumidorId).stream()
                .map(LiquidacionResponse::desde).toList());
    }
}
