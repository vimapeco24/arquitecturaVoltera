package com.voltera.tarifaeventos.infrastructure.rest;

import com.voltera.tarifaeventos.domain.event.ConsumoRegistrado;
import com.voltera.tarifaeventos.domain.model.CargoTarifa;
import com.voltera.tarifaeventos.domain.model.CargoVista;
import com.voltera.tarifaeventos.domain.model.EstadoCargo;
import com.voltera.tarifaeventos.domain.model.NotificacionLiquidada;
import com.voltera.tarifaeventos.domain.port.in.ActivarMedidorUseCase;
import com.voltera.tarifaeventos.domain.port.in.ConsultarCargoUseCase;
import com.voltera.tarifaeventos.domain.port.in.ProcesarConsumoUseCase;
import com.voltera.tarifaeventos.infrastructure.rest.dto.ActivarMedidorRequest;
import com.voltera.tarifaeventos.infrastructure.rest.dto.CargoVistaResponse;
import com.voltera.tarifaeventos.infrastructure.rest.dto.NotificacionResponse;
import com.voltera.tarifaeventos.infrastructure.rest.dto.SimularEventoRequest;
import com.voltera.tarifaeventos.infrastructure.rest.dto.StatsResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * API REST del bounded context Tarifa-Eventos. Las consultas se sirven del lado
 * de LECTURA (CQRS). El endpoint de simulacion inyecta un evento sin broker; el
 * de activacion genera la notificacion liquidada.
 */
@RestController
@RequestMapping("/api/v1/cargos")
public class CargoController {

    private final ConsultarCargoUseCase consultarUseCase;
    private final ProcesarConsumoUseCase procesarUseCase;
    private final ActivarMedidorUseCase activarUseCase;

    public CargoController(ConsultarCargoUseCase consultarUseCase,
                           ProcesarConsumoUseCase procesarUseCase,
                           ActivarMedidorUseCase activarUseCase) {
        this.consultarUseCase = consultarUseCase;
        this.procesarUseCase = procesarUseCase;
        this.activarUseCase = activarUseCase;
    }

    @GetMapping("/{id}")
    public CargoVistaResponse porId(@PathVariable String id) {
        return CargoVistaResponse.desde(consultarUseCase.porId(id));
    }

    @GetMapping
    public List<CargoVistaResponse> listar(@RequestParam(required = false) String prosumidorId) {
        List<CargoVista> vistas = (prosumidorId != null && !prosumidorId.isBlank())
                ? consultarUseCase.porProsumidor(prosumidorId)
                : consultarUseCase.todos();
        return vistas.stream().map(CargoVistaResponse::desde).toList();
    }

    @GetMapping("/stats")
    public StatsResponse stats() {
        List<CargoVista> todos = consultarUseCase.todos();

        long liquidados = todos.stream().filter(v -> v.estado() == EstadoCargo.LIQUIDADO).count();
        long sinCargo = todos.stream().filter(v -> v.estado() == EstadoCargo.SIN_CARGO).count();

        BigDecimal montoTotal = todos.stream()
                .map(CargoVista::montoCargo)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal excedenteTotal = todos.stream()
                .map(CargoVista::excedenteKwh)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new StatsResponse(todos.size(), liquidados, sinCargo, montoTotal, excedenteTotal);
    }

    /** Inyecta un ConsumoRegistrado directamente al caso de uso (sin broker). */
    @PostMapping("/simular-evento")
    public CargoVistaResponse simularEvento(@Valid @RequestBody SimularEventoRequest request) {
        Instant ocurrido = request.ocurridoEn() != null ? request.ocurridoEn() : Instant.now();
        Instant emitido = request.emitidoEn() != null ? request.emitidoEn() : Instant.now();
        ConsumoRegistrado evento = new ConsumoRegistrado(
                request.eventId(),
                request.medidorId(),
                request.prosumidorId(),
                request.consumoKwh(),
                request.umbralKwh(),
                request.consumoExtra(),
                ocurrido,
                emitido
        );
        CargoTarifa cargo = procesarUseCase.procesar(evento);
        return CargoVistaResponse.desde(consultarUseCase.porId(cargo.getId().valor()));
    }

    /**
     * Activa un medidor/prosumidor y genera la notificacion liquidada (factura,
     * tarifas) publicada en el topico de notificacion.
     */
    @PostMapping("/activar-medidor")
    public NotificacionResponse activarMedidor(@Valid @RequestBody ActivarMedidorRequest request) {
        NotificacionLiquidada notificacion = activarUseCase.activar(
                new ActivarMedidorUseCase.ComandoActivarMedidor(
                        request.medidorId(), request.prosumidorId(), request.umbralKwh()));
        return NotificacionResponse.desde(notificacion);
    }
}
