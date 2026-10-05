package com.voltera.tarifaeventos.infrastructure.rest;

import com.voltera.tarifaeventos.domain.model.SagaAltaMedidor;
import com.voltera.tarifaeventos.domain.port.in.OrquestarAltaMedidorUseCase;
import com.voltera.tarifaeventos.domain.port.out.InboxPort;
import com.voltera.tarifaeventos.domain.port.out.OutboxPort;
import com.voltera.tarifaeventos.domain.port.out.SagaRepositoryPort;
import com.voltera.tarifaeventos.infrastructure.rest.dto.IniciarAltaRequest;
import com.voltera.tarifaeventos.infrastructure.rest.dto.InboxResponse;
import com.voltera.tarifaeventos.infrastructure.rest.dto.OutboxResponse;
import com.voltera.tarifaeventos.infrastructure.rest.dto.SagaResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * API REST del patron <b>Orquestacion (mediador)</b> de la lamina 10.
 *
 * <p>Permite dirigir una saga de alta de medidor y observar los patrones de soporte
 * (OUTBOX transaccional e INBOX de idempotencia):</p>
 * <ul>
 *   <li>{@code POST /saga/alta}                      inicia el alta (emite MedidorHabilitado via outbox)</li>
 *   <li>{@code POST /saga/{id}/canal-ingesta-creado} respuesta del servicio de Ingesta</li>
 *   <li>{@code POST /saga/{id}/tarifa-asignada}      respuesta del servicio de Tarifas</li>
 *   <li>{@code POST /saga/timeouts}                  fuerza la revision de timeouts (15 min)</li>
 *   <li>{@code GET  /saga} / {@code GET /saga/{id}}  consulta de estado</li>
 *   <li>{@code GET  /saga/outbox}                    filas del outbox transaccional</li>
 *   <li>{@code GET  /saga/inbox}                     tabla inbox de idempotencia</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/orquestacion")
public class OrquestacionController {

    private final OrquestarAltaMedidorUseCase orquestador;
    private final SagaRepositoryPort sagas;
    private final OutboxPort outbox;
    private final InboxPort inbox;

    public OrquestacionController(OrquestarAltaMedidorUseCase orquestador,
                                  SagaRepositoryPort sagas,
                                  OutboxPort outbox,
                                  InboxPort inbox) {
        this.orquestador = orquestador;
        this.sagas = sagas;
        this.outbox = outbox;
        this.inbox = inbox;
    }

    @PostMapping("/saga/alta")
    public SagaResponse iniciarAlta(@Valid @RequestBody IniciarAltaRequest request) {
        SagaAltaMedidor saga = orquestador.iniciarAlta(request.medidorId(), request.prosumidorId());
        return SagaResponse.desde(saga);
    }

    @PostMapping("/saga/{id}/canal-ingesta-creado")
    public SagaResponse canalIngestaCreado(@PathVariable String id) {
        return SagaResponse.desde(orquestador.registrarCanalIngestaCreado(id));
    }

    @PostMapping("/saga/{id}/tarifa-asignada")
    public SagaResponse tarifaAsignada(@PathVariable String id) {
        return SagaResponse.desde(orquestador.registrarTarifaAsignada(id));
    }

    @PostMapping("/saga/timeouts")
    public Map<String, Object> procesarTimeouts() {
        int compensadas = orquestador.procesarTimeouts();
        return Map.of("compensadas", compensadas);
    }

    @PostMapping("/saga/{id}/forzar-timeout")
    public SagaResponse forzarTimeout(@PathVariable String id) {
        return SagaResponse.desde(orquestador.forzarTimeout(id));
    }

    @GetMapping("/saga/{id}")
    public SagaResponse porId(@PathVariable String id) {
        return sagas.buscarPorId(id)
                .map(SagaResponse::desde)
                .orElseThrow(() -> new com.voltera.tarifaeventos.domain.exception.SagaNoEncontradaException(id));
    }

    @GetMapping("/saga")
    public List<SagaResponse> todas() {
        return sagas.todas().stream().map(SagaResponse::desde).toList();
    }

    @GetMapping("/saga/outbox")
    public List<OutboxResponse> outbox() {
        return outbox.todos().stream().map(OutboxResponse::desde).toList();
    }

    @GetMapping("/saga/inbox")
    public InboxResponse inbox() {
        return InboxResponse.desde(inbox.totalUnicos(), inbox.totalDuplicados(), inbox.todos());
    }
}
