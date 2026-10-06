package com.voltera.ingesta.infrastructure.rest;

import com.voltera.ingesta.domain.event.LecturaCrudaRecibida;
import com.voltera.ingesta.domain.model.ResultadoValidacion;
import com.voltera.ingesta.domain.model.SesionDeIngesta;
import com.voltera.ingesta.domain.port.in.IngestarLecturaUseCase;
import com.voltera.ingesta.domain.port.out.OutboxPort;
import com.voltera.ingesta.infrastructure.rest.dto.LecturaRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * API REST del BC Telemetria · Ingesta (lamina 02). Permite abrir/cerrar canal e
 * inyectar lecturas crudas para validar (prueba de regla + ventana de duplicados).
 */
@RestController
@RequestMapping("/api/v1/ingesta")
public class IngestaController {

    private final IngestarLecturaUseCase useCase;
    private final OutboxPort outbox;

    public IngestaController(IngestarLecturaUseCase useCase, OutboxPort outbox) {
        this.useCase = useCase;
        this.outbox = outbox;
    }

    @GetMapping("/sesion")
    public Map<String, Object> sesion() {
        SesionDeIngesta s = useCase.estado();
        return Map.of(
                "medidoresConCanal", s.totalMedidores(),
                "reglaMinKwh", s.regla().minKwh(),
                "reglaMaxKwh", s.regla().maxKwh()
        );
    }

    @PostMapping("/medidores/{serial}/abrir-canal")
    public Map<String, Object> abrir(@PathVariable String serial) {
        useCase.abrirCanal(serial);
        return Map.of("medidorSerial", serial, "canalAbierto", useCase.estado().canalAbierto(serial));
    }

    @PostMapping("/medidores/{serial}/cerrar-canal")
    public Map<String, Object> cerrar(@PathVariable String serial) {
        useCase.cerrarCanal(serial);
        return Map.of("medidorSerial", serial, "canalAbierto", useCase.estado().canalAbierto(serial));
    }

    @PostMapping("/lecturas")
    public Map<String, Object> ingestar(@Valid @RequestBody LecturaRequest req) {
        Instant capturada = req.capturadaEn() != null ? req.capturadaEn() : Instant.now();
        LecturaCrudaRecibida lectura = new LecturaCrudaRecibida(
                "EVT-" + UUID.randomUUID(), req.medidorSerial(), req.consumoKwh(),
                "REST", capturada, Instant.now());
        ResultadoValidacion r = useCase.procesar(lectura);
        return Map.of("medidorSerial", req.medidorSerial(), "resultado", r.name());
    }

    @GetMapping("/outbox")
    public List<?> outbox() {
        return outbox.todos();
    }
}
