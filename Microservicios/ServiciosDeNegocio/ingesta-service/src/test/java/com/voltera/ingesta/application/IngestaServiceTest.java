package com.voltera.ingesta.application;

import com.voltera.ingesta.domain.event.LecturaCrudaRecibida;
import com.voltera.ingesta.domain.model.MensajeOutbox;
import com.voltera.ingesta.domain.model.ReglaDeValidacion;
import com.voltera.ingesta.domain.model.ResultadoValidacion;
import com.voltera.ingesta.domain.model.SesionDeIngesta;
import com.voltera.ingesta.domain.port.out.SerializadorEventosPort;
import com.voltera.ingesta.domain.port.out.SesionRepositoryPort;
import com.voltera.ingesta.infrastructure.persistence.InMemoryOutbox;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("IngestaService - validacion, duplicados y canal (lamina 02)")
class IngestaServiceTest {

    private SesionDeIngesta sesion;
    private InMemoryOutbox outbox;
    private IngestaService service;

    static class FakeRepo implements SesionRepositoryPort {
        final SesionDeIngesta s;
        FakeRepo(SesionDeIngesta s) { this.s = s; }
        @Override public SesionDeIngesta obtener() { return s; }
    }
    static class FakeSerializador implements SerializadorEventosPort {
        @Override public String aJson(Object payload) { return String.valueOf(payload); }
    }

    @BeforeEach
    void setUp() {
        sesion = new SesionDeIngesta(new ReglaDeValidacion(0.0, 100.0));
        outbox = new InMemoryOutbox();
        service = new IngestaService(new FakeRepo(sesion), outbox, new FakeSerializador());
    }

    private LecturaCrudaRecibida lectura(String serial, double kwh, Instant t) {
        return new LecturaCrudaRecibida("EVT-" + serial + t.toEpochMilli(), serial, kwh, "Landis", t, Instant.now());
    }

    private List<String> tiposOutbox() {
        return outbox.todos().stream().map(MensajeOutbox::tipoEvento).toList();
    }

    @Test
    @DisplayName("abrirCanal emite CanalIngestaCreado solo la primera vez")
    void abrirCanalEmiteUnaVez() {
        service.abrirCanal("SER-1");
        service.abrirCanal("SER-1"); // idempotente
        assertEquals(1, tiposOutbox().stream().filter("CanalIngestaCreado"::equals).count());
    }

    @Test
    @DisplayName("Lectura en rango y canal abierto -> VALIDADA + LecturaValidada")
    void lecturaValida() {
        service.abrirCanal("SER-2");
        ResultadoValidacion r = service.procesar(lectura("SER-2", 10.0, Instant.now()));
        assertEquals(ResultadoValidacion.VALIDADA, r);
        assertTrue(tiposOutbox().contains("LecturaValidada"));
    }

    @Test
    @DisplayName("Lectura fuera de rango -> SOSPECHOSA + LecturaSospechosaDetectada")
    void lecturaSospechosa() {
        service.abrirCanal("SER-3");
        ResultadoValidacion r = service.procesar(lectura("SER-3", 999.0, Instant.now()));
        assertEquals(ResultadoValidacion.SOSPECHOSA, r);
        assertTrue(tiposOutbox().contains("LecturaSospechosaDetectada"));
    }

    @Test
    @DisplayName("Misma lectura (serial+timestamp) repetida -> DUPLICADA, no se emite dos veces")
    void duplicadaNoSeReemite() {
        service.abrirCanal("SER-4");
        Instant t = Instant.now();
        assertEquals(ResultadoValidacion.VALIDADA, service.procesar(lectura("SER-4", 5.0, t)));
        assertEquals(ResultadoValidacion.DUPLICADA, service.procesar(lectura("SER-4", 5.0, t)));
        assertEquals(1, tiposOutbox().stream().filter("LecturaValidada"::equals).count());
    }

    @Test
    @DisplayName("Lectura de medidor sin canal abierto -> NO_HABILITADO (se ignora)")
    void noHabilitadoSeIgnora() {
        ResultadoValidacion r = service.procesar(lectura("SER-X", 10.0, Instant.now()));
        assertEquals(ResultadoValidacion.NO_HABILITADO, r);
        assertTrue(tiposOutbox().isEmpty());
    }
}
