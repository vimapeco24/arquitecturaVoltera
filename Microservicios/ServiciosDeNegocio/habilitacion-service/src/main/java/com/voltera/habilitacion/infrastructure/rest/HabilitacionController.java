package com.voltera.habilitacion.infrastructure.rest;

import com.voltera.habilitacion.domain.event.OrdenInstalacionCerrada;
import com.voltera.habilitacion.domain.port.in.HabilitarMedidorUseCase;
import com.voltera.habilitacion.domain.port.out.MedidorRepositoryPort;
import com.voltera.habilitacion.domain.port.out.OutboxPort;
import com.voltera.habilitacion.infrastructure.rest.dto.HabilitarRequest;
import com.voltera.habilitacion.infrastructure.rest.dto.MedidorResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * API REST del BC Habilitacion de Medidores (lamina 02). Permite disparar el alta
 * (equivalente a OrdenInstalacionCerrada), confirmar CanalIngestaCreado /
 * TarifaAsignada, forzar el timeout y consultar estado + outbox.
 */
@RestController
@RequestMapping("/api/v1/habilitacion")
public class HabilitacionController {

    private final HabilitarMedidorUseCase useCase;
    private final MedidorRepositoryPort repositorio;
    private final OutboxPort outbox;

    public HabilitacionController(HabilitarMedidorUseCase useCase,
                                  MedidorRepositoryPort repositorio,
                                  OutboxPort outbox) {
        this.useCase = useCase;
        this.repositorio = repositorio;
        this.outbox = outbox;
    }

    @PostMapping("/medidores")
    public MedidorResponse habilitar(@Valid @RequestBody HabilitarRequest req) {
        OrdenInstalacionCerrada orden = new OrdenInstalacionCerrada(
                "EVT-" + UUID.randomUUID(),
                req.ordenInstalacionId() != null ? req.ordenInstalacionId() : "ORD-" + UUID.randomUUID(),
                req.medidorId(), req.serial(), req.fabricante(),
                req.codigoPunto(), req.direccion(), Instant.now());
        return MedidorResponse.desde(useCase.habilitarDesdeOrden(orden));
    }

    @PostMapping("/medidores/{id}/canal-ingesta-creado")
    public MedidorResponse canalIngesta(@PathVariable String id) {
        return MedidorResponse.desde(useCase.registrarCanalIngestaCreado(id));
    }

    @PostMapping("/medidores/{id}/tarifa-asignada")
    public MedidorResponse tarifaAsignada(@PathVariable String id) {
        return MedidorResponse.desde(useCase.registrarTarifaAsignada(id));
    }

    @PostMapping("/medidores/{id}/forzar-timeout")
    public MedidorResponse forzarTimeout(@PathVariable String id) {
        return MedidorResponse.desde(useCase.forzarTimeout(id));
    }

    @GetMapping("/medidores/{id}")
    public MedidorResponse porId(@PathVariable String id) {
        return repositorio.buscarPorId(id)
                .map(MedidorResponse::desde)
                .orElseThrow(() -> new com.voltera.habilitacion.domain.exception.MedidorNoEncontradoException(id));
    }

    @GetMapping("/medidores")
    public List<MedidorResponse> todos() {
        return repositorio.todos().stream().map(MedidorResponse::desde).toList();
    }

    @GetMapping("/outbox")
    public List<?> outbox() {
        return outbox.todos();
    }
}
