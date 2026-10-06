package com.voltera.telemetriacore.infrastructure.rest;

import com.voltera.telemetriacore.domain.event.LecturaValidada;
import com.voltera.telemetriacore.domain.model.ConsumoVista;
import com.voltera.telemetriacore.domain.port.in.RegistrarConsumoUseCase;
import com.voltera.telemetriacore.domain.port.out.OutboxPort;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * API REST del BC Telemetria · Core (CQRS, lamina 02). Las consultas se sirven del
 * lado QUERY (vista materializada). El endpoint de simulacion inyecta una
 * LecturaValidada sin broker.
 */
@RestController
@RequestMapping("/api/v1/telemetria-core")
public class TelemetriaCoreController {

    private final RegistrarConsumoUseCase useCase;
    private final OutboxPort outbox;

    public TelemetriaCoreController(RegistrarConsumoUseCase useCase, OutboxPort outbox) {
        this.useCase = useCase;
        this.outbox = outbox;
    }

    @GetMapping("/consumo")
    public List<ConsumoVista> consumo(@RequestParam(required = false) String medidorSerial) {
        return (medidorSerial != null && !medidorSerial.isBlank())
                ? useCase.consumoPorMedidor(medidorSerial)
                : useCase.todas();
    }

    public record SimularLecturaRequest(@NotBlank String medidorSerial, double consumoKwh, Instant capturadaEn) {}

    @PostMapping("/simular-lectura")
    public Map<String, Object> simular(@RequestBody SimularLecturaRequest req) {
        Instant capturada = req.capturadaEn() != null ? req.capturadaEn() : Instant.now();
        useCase.crearSerie(req.medidorSerial());
        useCase.registrarLectura(new LecturaValidada(
                "EVT-" + UUID.randomUUID(), req.medidorSerial(), req.consumoKwh(),
                capturada, Instant.now()));
        return Map.of("medidorSerial", req.medidorSerial(), "lecturaRegistrada", true);
    }

    @GetMapping("/outbox")
    public List<?> outbox() {
        return outbox.todos();
    }
}
