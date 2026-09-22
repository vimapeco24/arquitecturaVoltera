package com.voltera.reaseguro.application;

import com.voltera.reaseguro.domain.event.SiniestroAprobado;
import com.voltera.reaseguro.domain.model.CesionRiesgo;
import com.voltera.reaseguro.domain.model.CesionVista;
import com.voltera.reaseguro.domain.model.EstadoCesion;
import com.voltera.reaseguro.infrastructure.persistence.InMemoryCesionRepository;
import com.voltera.reaseguro.infrastructure.persistence.InMemoryCesionVistaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ReaseguroServiceTest {

    private InMemoryCesionRepository escrituraRepo;
    private InMemoryCesionVistaRepository lecturaRepo;
    private ReaseguroService service;

    @BeforeEach
    void setUp() {
        escrituraRepo = new InMemoryCesionRepository();
        lecturaRepo = new InMemoryCesionVistaRepository();
        service = new ReaseguroService(escrituraRepo, lecturaRepo);
    }

    private SiniestroAprobado evento(String eventId, String polizaId, BigDecimal monto) {
        return new SiniestroAprobado(
                eventId,
                "SIN-1",
                polizaId,
                "PRO-1",
                monto,
                "Siniestro de prueba",
                Instant.now(),
                Instant.now()
        );
    }

    @Test
    @DisplayName("procesar evento crea cesion CEDIDA con monto cedido = 40% del aprobado")
    void procesarCreaCesionCedidaCon40Porciento() {
        CesionRiesgo cesion = service.procesar(evento("EVT-1", "POL-1", new BigDecimal("1000.00")));

        assertEquals(EstadoCesion.CEDIDA, cesion.getEstado());
        assertEquals(0, new BigDecimal("40").compareTo(cesion.getPorcentajeCedido()));
        assertEquals(0, new BigDecimal("400.00").compareTo(cesion.getMontoCedido()));

        // Se proyecto al lado de lectura.
        CesionVista vista = service.porId(cesion.getId().valor());
        assertEquals(EstadoCesion.CEDIDA, vista.estado());
        assertEquals(0, new BigDecimal("400.00").compareTo(vista.montoCedido()));
    }

    @Test
    @DisplayName("idempotencia: el mismo eventId no crea una cesion duplicada")
    void idempotenciaMismoEventIdNoDuplica() {
        CesionRiesgo primera = service.procesar(evento("EVT-DUP", "POL-1", new BigDecimal("500")));
        CesionRiesgo segunda = service.procesar(evento("EVT-DUP", "POL-1", new BigDecimal("500")));

        assertEquals(primera.getId().valor(), segunda.getId().valor());
        assertEquals(1, escrituraRepo.buscarTodas().size());
        assertEquals(1, lecturaRepo.buscarTodas().size());
    }

    @Test
    @DisplayName("consulta porId retorna la CesionVista del lado de lectura")
    void consultaRetornaVistaLectura() {
        CesionRiesgo cesion = service.procesar(evento("EVT-2", "POL-9", new BigDecimal("200")));

        CesionVista vista = service.porId(cesion.getId().valor());
        assertNotNull(vista);
        assertEquals(cesion.getId().valor(), vista.id());
        assertEquals("POL-9", vista.polizaId());

        List<CesionVista> porPoliza = service.porPoliza("POL-9");
        assertEquals(1, porPoliza.size());
    }

    @Test
    @DisplayName("stats: los totales agregados se calculan correctamente")
    void statsCalculaTotales() {
        service.procesar(evento("EVT-A", "POL-1", new BigDecimal("1000.00")));
        service.procesar(evento("EVT-B", "POL-2", new BigDecimal("2000.00")));

        List<CesionVista> todas = service.todas();
        assertEquals(2, todas.size());

        long cedidas = todas.stream().filter(v -> v.estado() == EstadoCesion.CEDIDA).count();
        assertEquals(2, cedidas);

        BigDecimal totalAprobado = todas.stream()
                .map(CesionVista::montoAprobado)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCedido = todas.stream()
                .map(CesionVista::montoCedido)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        assertEquals(0, new BigDecimal("3000.00").compareTo(totalAprobado));
        assertEquals(0, new BigDecimal("1200.00").compareTo(totalCedido));
    }
}
