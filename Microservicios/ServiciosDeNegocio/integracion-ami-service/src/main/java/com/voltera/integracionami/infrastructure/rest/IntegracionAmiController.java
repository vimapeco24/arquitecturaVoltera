package com.voltera.integracionami.infrastructure.rest;

import com.voltera.integracionami.domain.model.ConexionHeadEnd;
import com.voltera.integracionami.domain.model.LecturaCruda;
import com.voltera.integracionami.domain.port.in.RecibirLecturasUseCase;
import com.voltera.integracionami.domain.port.out.OutboxPort;
import com.voltera.integracionami.infrastructure.rest.dto.LoteLecturasRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * API REST del BC Integracion AMI (lamina 02). Permite inyectar lotes de lecturas
 * crudas (ACL), gestionar que medidores atiende y reportar degradacion.
 */
@RestController
@RequestMapping("/api/v1/ami")
public class IntegracionAmiController {

    private final RecibirLecturasUseCase useCase;
    private final OutboxPort outbox;

    public IntegracionAmiController(RecibirLecturasUseCase useCase, OutboxPort outbox) {
        this.useCase = useCase;
        this.outbox = outbox;
    }

    @GetMapping("/conexion")
    public Map<String, Object> conexion() {
        ConexionHeadEnd c = useCase.estado();
        return Map.of(
                "proveedorAmi", c.proveedorAmi(),
                "protocolo", c.protocolo().name(),
                "estado", c.estado().name(),
                "medidoresAtendidos", c.totalMedidores()
        );
    }

    @PostMapping("/lecturas-crudas")
    public Map<String, Object> recibirLote(@Valid @RequestBody LoteLecturasRequest req) {
        List<LecturaCruda> lote = req.lecturas().stream()
                .map(d -> new LecturaCruda(d.medidorSerial(), d.valor(), d.unidad(), d.capturadaEn()))
                .toList();
        String proveedor = req.proveedorAmi() != null ? req.proveedorAmi() : useCase.estado().proveedorAmi();
        int emitidas = useCase.recibirLote(proveedor, lote);
        return Map.of("recibidas", lote.size(), "emitidasCanonicas", emitidas);
    }

    @PostMapping("/medidores/{serial}/habilitar")
    public Map<String, Object> habilitar(@PathVariable String serial) {
        useCase.habilitarMedidor(serial);
        return Map.of("medidorSerial", serial, "atendido", true);
    }

    @PostMapping("/medidores/{serial}/suspender")
    public Map<String, Object> suspender(@PathVariable String serial) {
        useCase.suspenderMedidor(serial);
        return Map.of("medidorSerial", serial, "atendido", false);
    }

    @PostMapping("/degradar")
    public Map<String, Object> degradar(@RequestParam(required = false, defaultValue = "Fallo del head-end") String motivo) {
        ConexionHeadEnd c = useCase.reportarDegradacion(motivo);
        return Map.of("proveedorAmi", c.proveedorAmi(), "estado", c.estado().name());
    }

    @GetMapping("/outbox")
    public List<?> outbox() {
        return outbox.todos();
    }
}
