package com.voltera.tarifaeventos.application;

import com.voltera.tarifaeventos.domain.event.ConsumoRegistrado;
import com.voltera.tarifaeventos.domain.model.CargoTarifa;
import com.voltera.tarifaeventos.domain.model.CargoVista;
import com.voltera.tarifaeventos.domain.model.EstadoCargo;
import com.voltera.tarifaeventos.domain.model.NotificacionLiquidada;
import com.voltera.tarifaeventos.domain.port.in.ActivarMedidorUseCase.ComandoActivarMedidor;
import com.voltera.tarifaeventos.domain.port.out.NotificacionPublisherPort;
import com.voltera.tarifaeventos.infrastructure.persistence.InMemoryCargoRepository;
import com.voltera.tarifaeventos.infrastructure.persistence.InMemoryCargoVistaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("TarifaEventosService - CQRS, idempotencia, consumo extra y notificacion")
class TarifaEventosServiceTest {

    private InMemoryCargoRepository escrituraRepo;
    private InMemoryCargoVistaRepository lecturaRepo;
    private NotificacionSpy notificacionPublisher;
    private TarifaEventosService service;

    static class NotificacionSpy implements NotificacionPublisherPort {
        final List<NotificacionLiquidada> publicadas = new ArrayList<>();
        @Override public void publicar(NotificacionLiquidada n) { publicadas.add(n); }
    }

    @BeforeEach
    void setUp() {
        escrituraRepo = new InMemoryCargoRepository();
        lecturaRepo = new InMemoryCargoVistaRepository();
        notificacionPublisher = new NotificacionSpy();
        service = new TarifaEventosService(escrituraRepo, lecturaRepo, notificacionPublisher);
    }

    private ConsumoRegistrado evento(String eventId, BigDecimal consumo, BigDecimal umbral, boolean extra) {
        return new ConsumoRegistrado(eventId, "MED-1", "PRO-1", consumo, umbral, extra,
                Instant.now(), Instant.now());
    }

    @Test
    @DisplayName("Consumo extra: crea cargo LIQUIDADO con monto = excedente * 850 y lo proyecta a lectura")
    void consumoExtraGeneraCargoLiquidado() {
        // consumo 150, umbral 100 -> excedente 50 * 850 = 42500.00
        CargoTarifa cargo = service.procesar(evento("EVT-1", new BigDecimal("150"), new BigDecimal("100"), true));

        assertEquals(EstadoCargo.LIQUIDADO, cargo.getEstado());
        assertEquals(0, new BigDecimal("50").compareTo(cargo.getExcedenteKwh()));
        assertEquals(0, new BigDecimal("42500.00").compareTo(cargo.getMontoCargo()));

        // Proyectado al lado de lectura (CQRS).
        CargoVista vista = service.porId(cargo.getId().valor());
        assertEquals(EstadoCargo.LIQUIDADO, vista.estado());
        assertEquals(0, new BigDecimal("42500.00").compareTo(vista.montoCargo()));
    }

    @Test
    @DisplayName("Sin consumo extra: cargo SIN_CARGO con monto 0")
    void sinConsumoExtraNoCobra() {
        CargoTarifa cargo = service.procesar(evento("EVT-2", new BigDecimal("80"), new BigDecimal("100"), false));

        assertEquals(EstadoCargo.SIN_CARGO, cargo.getEstado());
        assertEquals(0, BigDecimal.ZERO.compareTo(cargo.getMontoCargo()));
        assertEquals(0, BigDecimal.ZERO.compareTo(cargo.getExcedenteKwh()));
    }

    @Test
    @DisplayName("Idempotencia: el mismo eventId no crea un cargo duplicado")
    void idempotenciaMismoEventIdNoDuplica() {
        CargoTarifa primero = service.procesar(evento("EVT-DUP", new BigDecimal("150"), new BigDecimal("100"), true));
        CargoTarifa segundo = service.procesar(evento("EVT-DUP", new BigDecimal("150"), new BigDecimal("100"), true));

        assertEquals(primero.getId().valor(), segundo.getId().valor());
        assertEquals(1, escrituraRepo.buscarTodos().size());
        assertEquals(1, lecturaRepo.buscarTodas().size());
    }

    @Test
    @DisplayName("Activar medidor genera y publica la notificacion liquidada con factura y tarifas")
    void activarMedidorGeneraNotificacion() {
        NotificacionLiquidada n = service.activar(
                new ComandoActivarMedidor("MED-9", "PRO-9", new BigDecimal("120")));

        assertEquals("MEDIDOR_ACTIVADO", n.tipo());
        assertEquals("MED-9", n.medidorId());
        assertEquals(0, new BigDecimal("120").compareTo(n.umbralKwh()));
        assertNotNull(n.mensaje());
        // Se publico en el topico de notificacion.
        assertEquals(1, notificacionPublisher.publicadas.size());
    }
}
