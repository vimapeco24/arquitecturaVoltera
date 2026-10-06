package com.voltera.telemetriacore.application;

import com.voltera.telemetriacore.domain.event.LecturaValidada;
import com.voltera.telemetriacore.domain.model.ConsumoVista;
import com.voltera.telemetriacore.domain.model.MensajeOutbox;
import com.voltera.telemetriacore.domain.port.out.SerializadorEventosPort;
import com.voltera.telemetriacore.infrastructure.persistence.InMemoryConsumoVistaRepository;
import com.voltera.telemetriacore.infrastructure.persistence.InMemoryOutbox;
import com.voltera.telemetriacore.infrastructure.persistence.InMemorySerieRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("TelemetriaCoreService - CQRS, agregacion por intervalo y sin reporte (lamina 02)")
class TelemetriaCoreServiceTest {

    private InMemorySerieRepository series;
    private InMemoryConsumoVistaRepository vistas;
    private InMemoryOutbox outbox;
    private TelemetriaCoreService service;

    static class FakeSerializador implements SerializadorEventosPort {
        @Override public String aJson(Object payload) { return String.valueOf(payload); }
    }

    @BeforeEach
    void setUp() {
        series = new InMemorySerieRepository();
        vistas = new InMemoryConsumoVistaRepository();
        outbox = new InMemoryOutbox();
        // intervalo 15 min, sin-reporte 30 min
        service = new TelemetriaCoreService(series, vistas, outbox, new FakeSerializador(), 15, 30);
    }

    private LecturaValidada lectura(String serial, double kwh, Instant t) {
        return new LecturaValidada("EVT-" + t.toEpochMilli(), serial, kwh, t, Instant.now());
    }

    private List<String> tiposOutbox() {
        return outbox.todos().stream().map(MensajeOutbox::tipoEvento).toList();
    }

    @Test
    @DisplayName("Lecturas en el mismo intervalo no cierran; una del siguiente intervalo cierra y emite ConsumoIntervaloRegistrado")
    void cierreDeIntervaloEmiteEvento() {
        Instant base = Instant.parse("2026-01-01T10:00:00Z");
        service.registrarLectura(lectura("SER-1", 2.0, base));                 // intervalo 10:00-10:15
        service.registrarLectura(lectura("SER-1", 3.0, base.plusSeconds(300))); // mismo intervalo (10:05)
        // aun no se cierra nada
        assertFalse(tiposOutbox().contains("ConsumoIntervaloRegistrado"));

        service.registrarLectura(lectura("SER-1", 1.0, base.plusSeconds(16 * 60))); // 10:16 -> nuevo intervalo
        // se cerro el intervalo anterior con 2+3=5 kWh
        assertTrue(tiposOutbox().contains("ConsumoIntervaloRegistrado"));

        List<ConsumoVista> v = service.consumoPorMedidor("SER-1");
        assertEquals(1, v.size());
        assertEquals(5.0, v.get(0).consumoNetoKwh(), 1e-9);
        assertEquals(2, v.get(0).numLecturas());
    }

    @Test
    @DisplayName("crearSerie es idempotente (MedidorHabilitado repetido)")
    void crearSerieIdempotente() {
        service.crearSerie("SER-2");
        service.crearSerie("SER-2");
        assertTrue(series.buscarPorMedidor("SER-2").isPresent());
    }

    @Test
    @DisplayName("revisarSinReporte emite MedidorSinReporte si la ultima lectura supero la ventana")
    void sinReporteEmiteEvento() {
        Instant hace1h = Instant.now().minusSeconds(3600);
        service.registrarLectura(lectura("SER-3", 1.0, hace1h)); // ultima lectura hace 1h > 30 min
        int n = service.revisarSinReporte();
        assertEquals(1, n);
        assertTrue(tiposOutbox().contains("MedidorSinReporte"));
    }

    @Test
    @DisplayName("La vista agregada acumula el total por medidor al cerrar intervalos (lamina 03: vistas agregadas)")
    void vistaAgregadaAcumula() {
        Instant base = Instant.parse("2026-01-01T10:00:00Z");
        // Intervalo 1 (10:00-10:15): 2 + 3 = 5 kWh, se cierra al llegar 10:16
        service.registrarLectura(lectura("SER-9", 2.0, base));
        service.registrarLectura(lectura("SER-9", 3.0, base.plusSeconds(300)));
        service.registrarLectura(lectura("SER-9", 4.0, base.plusSeconds(16 * 60)));   // cierra intervalo 1
        // Intervalo 2 (10:15-10:30): 4 kWh acumulado; se cierra al llegar 10:31
        service.registrarLectura(lectura("SER-9", 1.0, base.plusSeconds(31 * 60)));   // cierra intervalo 2 (=5)

        var agregado = service.agregadoPorMedidor("SER-9");
        assertTrue(agregado.isPresent());
        // intervalo 1 = 5 kWh, intervalo 2 = 4 kWh => total 9 kWh en 2 intervalos
        assertEquals(2, agregado.get().intervalosRegistrados());
        assertEquals(9.0, agregado.get().consumoTotalKwh(), 1e-9);
        // y en la lista de agregados tambien aparece
        assertEquals(1, service.agregados().size());
    }
}
