package com.voltera.liquidacion.infrastructure.rest;

import com.voltera.liquidacion.domain.port.in.ConsultarTransaccionUseCase;
import com.voltera.liquidacion.domain.port.in.EmparejarOrdenesUseCase;
import com.voltera.liquidacion.infrastructure.rest.dto.EmparejarRequest;
import com.voltera.liquidacion.infrastructure.rest.dto.TransaccionResponse;
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
 * Adaptador de ENTRADA (driving adapter): expone los casos de uso de
 * liquidacion P2P via HTTP/REST.
 */
@RestController
@RequestMapping("/api/v1/transacciones")
public class LiquidacionController {

    private final EmparejarOrdenesUseCase emparejarOrdenesUseCase;
    private final ConsultarTransaccionUseCase consultarTransaccionUseCase;

    public LiquidacionController(EmparejarOrdenesUseCase emparejarOrdenesUseCase,
                                 ConsultarTransaccionUseCase consultarTransaccionUseCase) {
        this.emparejarOrdenesUseCase = emparejarOrdenesUseCase;
        this.consultarTransaccionUseCase = consultarTransaccionUseCase;
    }

    @PostMapping("/emparejar")
    public ResponseEntity<TransaccionResponse> emparejar(@Valid @RequestBody EmparejarRequest req) {
        var comando = new EmparejarOrdenesUseCase.ComandoEmparejar(
                req.vendedorId(),
                req.kwhVenta(),
                req.precioVenta(),
                req.compradorId(),
                req.kwhCompra(),
                req.precioCompra()
        );
        var transaccion = emparejarOrdenesUseCase.emparejar(comando);
        return ResponseEntity.status(HttpStatus.CREATED).body(TransaccionResponse.desde(transaccion));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TransaccionResponse> porId(@PathVariable String id) {
        var transaccion = consultarTransaccionUseCase.porId(id);
        return ResponseEntity.ok(TransaccionResponse.desde(transaccion));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TransaccionResponse> actualizar(@PathVariable String id,
                                                          @Valid @RequestBody EmparejarRequest req) {
        var comando = new EmparejarOrdenesUseCase.ComandoEmparejar(
                req.vendedorId(),
                req.kwhVenta(),
                req.precioVenta(),
                req.compradorId(),
                req.kwhCompra(),
                req.precioCompra()
        );
        var transaccion = emparejarOrdenesUseCase.actualizar(id, comando);
        return ResponseEntity.ok(TransaccionResponse.desde(transaccion));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable String id) {
        emparejarOrdenesUseCase.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<TransaccionResponse>> porProsumidor(@RequestParam String prosumidorId) {
        var lista = consultarTransaccionUseCase.porProsumidor(prosumidorId).stream()
                .map(TransaccionResponse::desde)
                .toList();
        return ResponseEntity.ok(lista);
    }
}
