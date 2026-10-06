package com.voltera.notificaciones.infrastructure.rest;

import com.voltera.notificaciones.domain.event.LecturaSospechosaDetectada;
import com.voltera.notificaciones.domain.event.MedidorHabilitado;
import com.voltera.notificaciones.domain.event.MedidorSinReporte;
import com.voltera.notificaciones.domain.model.Notificacion;
import com.voltera.notificaciones.domain.port.in.NotificarClienteUseCase;
import com.voltera.notificaciones.domain.port.out.OutboxPort;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * API REST del BC Notificaciones (lamina 02). Consultas del historial y endpoints
 * de simulacion que inyectan los eventos de entrada sin broker.
 */
@RestController
@RequestMapping("/api/v1/notificaciones")
public class NotificacionController {

    private final NotificarClienteUseCase useCase;
    private final OutboxPort outbox;

    public NotificacionController(NotificarClienteUseCase useCase, OutboxPort outbox) {
        this.useCase = useCase;
        this.outbox = outbox;
    }

    @GetMapping
    public List<Notificacion> historial() {
        return useCase.historial();
    }

    public record SimularHabilitadoRequest(@NotBlank String medidorId, String serial) {}

    @PostMapping("/simular/medidor-habilitado")
    public Notificacion simularHabilitado(@RequestBody SimularHabilitadoRequest req) {
        return useCase.notificarMedidorHabilitado(
                new MedidorHabilitado(req.medidorId(), req.serial(), "ACTIVADO"));
    }

    public record SimularSospechosaRequest(@NotBlank String medidorSerial, double consumoKwh, String motivo) {}

    @PostMapping("/simular/lectura-sospechosa")
    public Notificacion simularSospechosa(@RequestBody SimularSospechosaRequest req) {
        return useCase.notificarLecturaSospechosa(
                new LecturaSospechosaDetectada(req.medidorSerial(), req.consumoKwh(), req.motivo()));
    }

    public record SimularSinReporteRequest(@NotBlank String medidorSerial, String ultimaLecturaEn) {}

    @PostMapping("/simular/medidor-sin-reporte")
    public Notificacion simularSinReporte(@RequestBody SimularSinReporteRequest req) {
        return useCase.notificarMedidorSinReporte(
                new MedidorSinReporte(req.medidorSerial(), req.ultimaLecturaEn()));
    }

    @GetMapping("/outbox")
    public List<?> outbox() {
        return outbox.todos();
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "UP", "servicio", "notificaciones-service");
    }
}
