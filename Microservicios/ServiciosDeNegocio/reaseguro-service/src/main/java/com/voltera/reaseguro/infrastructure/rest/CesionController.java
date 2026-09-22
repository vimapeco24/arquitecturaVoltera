package com.voltera.reaseguro.infrastructure.rest;

import com.voltera.reaseguro.domain.event.SiniestroAprobado;
import com.voltera.reaseguro.domain.model.CesionRiesgo;
import com.voltera.reaseguro.domain.model.CesionVista;
import com.voltera.reaseguro.domain.model.EstadoCesion;
import com.voltera.reaseguro.domain.port.in.ConsultarCesionUseCase;
import com.voltera.reaseguro.domain.port.in.ProcesarEventoSiniestroUseCase;
import com.voltera.reaseguro.infrastructure.rest.dto.CesionVistaResponse;
import com.voltera.reaseguro.infrastructure.rest.dto.SimularEventoRequest;
import com.voltera.reaseguro.infrastructure.rest.dto.StatsResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * API REST del bounded context Reaseguro. Las consultas se sirven del lado de
 * LECTURA (CQRS). El endpoint de simulacion permite inyectar un evento sin broker.
 */
@RestController
@RequestMapping("/api/v1/cesiones")
public class CesionController {

    private final ConsultarCesionUseCase consultarUseCase;
    private final ProcesarEventoSiniestroUseCase procesarUseCase;

    public CesionController(ConsultarCesionUseCase consultarUseCase,
                            ProcesarEventoSiniestroUseCase procesarUseCase) {
        this.consultarUseCase = consultarUseCase;
        this.procesarUseCase = procesarUseCase;
    }

    @GetMapping("/{id}")
    public CesionVistaResponse porId(@PathVariable String id) {
        return CesionVistaResponse.desde(consultarUseCase.porId(id));
    }

    @GetMapping
    public List<CesionVistaResponse> listar(@RequestParam(required = false) String polizaId) {
        List<CesionVista> vistas = (polizaId != null && !polizaId.isBlank())
                ? consultarUseCase.porPoliza(polizaId)
                : consultarUseCase.todas();
        return vistas.stream().map(CesionVistaResponse::desde).toList();
    }

    @GetMapping("/stats")
    public StatsResponse stats() {
        List<CesionVista> todas = consultarUseCase.todas();

        long totalCedidas = todas.stream().filter(v -> v.estado() == EstadoCesion.CEDIDA).count();
        long totalPendientes = todas.stream().filter(v -> v.estado() == EstadoCesion.PENDIENTE).count();
        long totalRechazadas = todas.stream().filter(v -> v.estado() == EstadoCesion.RECHAZADA_REASEGURO).count();

        BigDecimal montoTotalAprobado = todas.stream()
                .map(CesionVista::montoAprobado)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal montoTotalCedido = todas.stream()
                .map(CesionVista::montoCedido)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new StatsResponse(
                todas.size(),
                totalCedidas,
                totalPendientes,
                totalRechazadas,
                montoTotalAprobado,
                montoTotalCedido
        );
    }

    @PostMapping("/simular-evento")
    public CesionVistaResponse simularEvento(@Valid @RequestBody SimularEventoRequest request) {
        SiniestroAprobado evento = new SiniestroAprobado(
                request.eventId(),
                request.siniestroId(),
                request.polizaId(),
                request.prosumidorId(),
                request.montoAprobado(),
                request.descripcion(),
                request.aprobadoEn(),
                request.emitidoEn()
        );
        CesionRiesgo cesion = procesarUseCase.procesar(evento);
        return CesionVistaResponse.desde(consultarUseCase.porId(cesion.getId().valor()));
    }
}
