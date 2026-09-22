package com.voltera.volumetria.infrastructure.rest;

import com.voltera.volumetria.domain.port.in.ConsultarLecturaUseCase;
import com.voltera.volumetria.domain.port.in.IngestarLecturaUseCase;
import com.voltera.volumetria.infrastructure.rest.dto.IngestarLecturaRequest;
import com.voltera.volumetria.infrastructure.rest.dto.LecturaResponse;
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
 * Adaptador de ENTRADA REST del servicio de Volumetria.
 */
@RestController
@RequestMapping("/api/v1/lecturas")
public class VolumetriaController {

    private final IngestarLecturaUseCase ingestarLecturaUseCase;
    private final ConsultarLecturaUseCase consultarLecturaUseCase;

    public VolumetriaController(IngestarLecturaUseCase ingestarLecturaUseCase,
                                ConsultarLecturaUseCase consultarLecturaUseCase) {
        this.ingestarLecturaUseCase = ingestarLecturaUseCase;
        this.consultarLecturaUseCase = consultarLecturaUseCase;
    }

    @PostMapping
    public ResponseEntity<LecturaResponse> ingestar(@Valid @RequestBody IngestarLecturaRequest req) {
        var comando = new IngestarLecturaUseCase.ComandoIngestarLectura(
                req.medidorId(), req.kwh(), req.direccion(), req.capturadaEn());
        var lectura = ingestarLecturaUseCase.ingestar(comando);
        return ResponseEntity.status(HttpStatus.CREATED).body(LecturaResponse.desde(lectura));
    }

    @GetMapping("/{id}")
    public ResponseEntity<LecturaResponse> porId(@PathVariable String id) {
        return ResponseEntity.ok(LecturaResponse.desde(consultarLecturaUseCase.porId(id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<LecturaResponse> actualizar(@PathVariable String id,
                                                      @Valid @RequestBody IngestarLecturaRequest req) {
        var comando = new IngestarLecturaUseCase.ComandoIngestarLectura(
                req.medidorId(), req.kwh(), req.direccion(), req.capturadaEn());
        var lectura = ingestarLecturaUseCase.actualizar(id, comando);
        return ResponseEntity.ok(LecturaResponse.desde(lectura));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable String id) {
        ingestarLecturaUseCase.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<LecturaResponse>> porMedidor(@RequestParam String medidorId) {
        var lista = consultarLecturaUseCase.porMedidor(medidorId).stream()
                .map(LecturaResponse::desde).toList();
        return ResponseEntity.ok(lista);
    }
}
