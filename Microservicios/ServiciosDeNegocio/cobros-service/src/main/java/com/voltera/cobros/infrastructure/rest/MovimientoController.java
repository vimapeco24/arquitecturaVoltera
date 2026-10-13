package com.voltera.cobros.infrastructure.rest;

import com.voltera.cobros.application.CobrosService;
import com.voltera.cobros.domain.event.PagoConfirmado;
import com.voltera.cobros.domain.model.Movimiento;
import com.voltera.cobros.domain.model.Prima;
import com.voltera.cobros.domain.port.in.ConsultarMovimientoUseCase;
import com.voltera.cobros.domain.port.in.ProcesarPagoConfirmadoUseCase;
import com.voltera.cobros.domain.port.in.RegistrarMovimientoUseCase;
import com.voltera.cobros.infrastructure.rest.dto.InboxResponse;
import com.voltera.cobros.infrastructure.rest.dto.MovimientoResponse;
import com.voltera.cobros.infrastructure.rest.dto.RegistrarMovimientoRequest;
import com.voltera.cobros.infrastructure.rest.dto.SimularPagoRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

/**
 * API REST del bounded context Cobros. Permite registrar movimientos de cobro,
 * consultarlos (lado lectura) e inyectar un {@code PagoConfirmado} simulado para
 * demostrar la conciliación idempotente sin necesidad de broker.
 */
@RestController
@RequestMapping("/api/v1/movimientos")
public class MovimientoController {

    private final RegistrarMovimientoUseCase registrarUseCase;
    private final ProcesarPagoConfirmadoUseCase procesarUseCase;
    private final ConsultarMovimientoUseCase consultarUseCase;
    private final CobrosService cobrosService;

    public MovimientoController(RegistrarMovimientoUseCase registrarUseCase,
                                ProcesarPagoConfirmadoUseCase procesarUseCase,
                                ConsultarMovimientoUseCase consultarUseCase,
                                CobrosService cobrosService) {
        this.registrarUseCase = registrarUseCase;
        this.procesarUseCase = procesarUseCase;
        this.consultarUseCase = consultarUseCase;
        this.cobrosService = cobrosService;
    }

    @PostMapping
    public ResponseEntity<MovimientoResponse> registrar(@Valid @RequestBody RegistrarMovimientoRequest req) {
        Prima.Periodicidad per = req.periodicidad() != null ? req.periodicidad() : Prima.Periodicidad.MENSUAL;
        Movimiento m = registrarUseCase.registrar(
                new RegistrarMovimientoUseCase.ComandoRegistrarMovimiento(
                        req.clienteId(), req.polizaId(), Prima.de(req.montoPrima(), per)));
        return ResponseEntity.status(HttpStatus.CREATED).body(MovimientoResponse.desde(m));
    }

    @GetMapping("/{id}")
    public MovimientoResponse porId(@PathVariable String id) {
        return MovimientoResponse.desde(consultarUseCase.porId(id));
    }

    @GetMapping
    public List<MovimientoResponse> listar(@RequestParam(required = false) String polizaId) {
        List<Movimiento> movs = (polizaId != null && !polizaId.isBlank())
                ? consultarUseCase.porPoliza(polizaId)
                : consultarUseCase.todos();
        return movs.stream().map(MovimientoResponse::desde).toList();
    }

    /** Inyecta un PagoConfirmado simulado (sin broker). Idempotente por eventId. */
    @PostMapping("/simular-pago")
    public MovimientoResponse simularPago(@Valid @RequestBody SimularPagoRequest req) {
        Instant confirmado = req.confirmadoEn() != null ? req.confirmadoEn() : Instant.now();
        Instant emitido = req.emitidoEn() != null ? req.emitidoEn() : Instant.now();
        PagoConfirmado evento = new PagoConfirmado(
                req.eventId(), req.referencia(), req.clienteId(), req.polizaId(),
                req.montoPagado(), confirmado, emitido);
        Movimiento m = procesarUseCase.procesar(evento);
        return MovimientoResponse.desde(m);
    }

    /** Tabla INBOX de idempotencia (únicos/duplicados). */
    @GetMapping("/inbox")
    public InboxResponse inbox() {
        return InboxResponse.desde(
                cobrosService.inboxUnicos(),
                cobrosService.inboxDuplicados(),
                cobrosService.inboxFilas());
    }
}
