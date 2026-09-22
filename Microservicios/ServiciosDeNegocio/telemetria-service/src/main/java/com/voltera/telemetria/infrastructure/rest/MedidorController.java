package com.voltera.telemetria.infrastructure.rest;

import com.voltera.telemetria.domain.port.in.ConsultarMedidorUseCase;
import com.voltera.telemetria.domain.port.in.DrenarBufferUseCase;
import com.voltera.telemetria.domain.port.in.GestionarMedidorUseCase;
import com.voltera.telemetria.domain.port.in.IngestarConsumoUseCase;
import com.voltera.telemetria.infrastructure.rest.dto.BufferResponse;
import com.voltera.telemetria.infrastructure.rest.dto.IngestaResponse;
import com.voltera.telemetria.infrastructure.rest.dto.IngestarConsumoRequest;
import com.voltera.telemetria.infrastructure.rest.dto.MedidorResponse;
import com.voltera.telemetria.infrastructure.rest.dto.RegistrarMedidorRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

/**
 * Adaptador de ENTRADA REST del servicio de Telemetria (emisor IoT).
 *
 * <ul>
 *   <li>Gestion del medidor: registrar, activar, suspender.</li>
 *   <li>Ingesta de consumo: al registrar una lectura se publica el evento
 *       ConsumoRegistrado al broker; si no hay conexion, va al buffer offline.</li>
 *   <li>Buffer: consultar y forzar el desencolado (store-and-forward).</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/medidores")
public class MedidorController {

    private final GestionarMedidorUseCase gestionarUseCase;
    private final IngestarConsumoUseCase ingestarUseCase;
    private final ConsultarMedidorUseCase consultarUseCase;
    private final DrenarBufferUseCase drenarBufferUseCase;

    public MedidorController(GestionarMedidorUseCase gestionarUseCase,
                             IngestarConsumoUseCase ingestarUseCase,
                             ConsultarMedidorUseCase consultarUseCase,
                             DrenarBufferUseCase drenarBufferUseCase) {
        this.gestionarUseCase = gestionarUseCase;
        this.ingestarUseCase = ingestarUseCase;
        this.consultarUseCase = consultarUseCase;
        this.drenarBufferUseCase = drenarBufferUseCase;
    }

    @PostMapping
    public ResponseEntity<MedidorResponse> registrar(@Valid @RequestBody RegistrarMedidorRequest req) {
        var comando = new GestionarMedidorUseCase.ComandoRegistrarMedidor(
                req.prosumidorId(), req.umbralKwh());
        var medidor = gestionarUseCase.registrar(comando);
        return ResponseEntity.status(HttpStatus.CREATED).body(MedidorResponse.desde(medidor));
    }

    @PostMapping("/{id}/activar")
    public ResponseEntity<MedidorResponse> activar(@PathVariable String id) {
        return ResponseEntity.ok(MedidorResponse.desde(gestionarUseCase.activar(id)));
    }

    @PostMapping("/{id}/suspender")
    public ResponseEntity<MedidorResponse> suspender(@PathVariable String id) {
        return ResponseEntity.ok(MedidorResponse.desde(gestionarUseCase.suspender(id)));
    }

    /** Ingesta de una lectura de consumo desde el medidor IoT. */
    @PostMapping("/{id}/lecturas")
    public ResponseEntity<IngestaResponse> ingestar(@PathVariable String id,
                                                    @Valid @RequestBody IngestarConsumoRequest req) {
        Instant capturada = req.capturadaEn() != null ? req.capturadaEn() : Instant.now();
        var comando = new IngestarConsumoUseCase.ComandoIngestarConsumo(id, req.consumoKwh(), capturada);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(IngestaResponse.desde(ingestarUseCase.ingestar(comando)));
    }

    /** Fuerza el desencolado del buffer offline (store-and-forward). */
    @PostMapping("/buffer/drenar")
    public ResponseEntity<BufferResponse> drenar() {
        return ResponseEntity.ok(BufferResponse.desde(drenarBufferUseCase.drenar()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<MedidorResponse> porId(@PathVariable String id) {
        return ResponseEntity.ok(MedidorResponse.desde(consultarUseCase.porId(id)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable String id) {
        gestionarUseCase.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<MedidorResponse>> listar(
            @RequestParam(name = "prosumidorId", required = false) String prosumidorId) {
        List<MedidorResponse> resultado = (prosumidorId != null && !prosumidorId.isBlank()
                ? consultarUseCase.porProsumidor(prosumidorId)
                : consultarUseCase.todos())
                .stream().map(MedidorResponse::desde).toList();
        return ResponseEntity.ok(resultado);
    }
}
