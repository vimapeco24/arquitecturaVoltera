package com.voltera.integracionami.application;

import com.voltera.integracionami.domain.model.ConexionHeadEnd;
import com.voltera.integracionami.domain.model.EstadoProveedor;
import com.voltera.integracionami.domain.model.LecturaCruda;
import com.voltera.integracionami.domain.model.MensajeOutbox;
import com.voltera.integracionami.domain.model.Protocolo;
import com.voltera.integracionami.domain.port.out.ConexionRepositoryPort;
import com.voltera.integracionami.domain.port.out.SerializadorEventosPort;
import com.voltera.integracionami.infrastructure.persistence.InMemoryOutbox;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("IntegracionAmiService - ACL, traduccion canonica y degradacion (lamina 02)")
class IntegracionAmiServiceTest {

    private ConexionHeadEnd conexion;
    private InMemoryOutbox outbox;
    private IntegracionAmiService service;

    static class FakeRepo implements ConexionRepositoryPort {
        ConexionHeadEnd c;
        FakeRepo(ConexionHeadEnd c) { this.c = c; }
        @Override public ConexionHeadEnd obtener() { return c; }
        @Override public ConexionHeadEnd guardar(ConexionHeadEnd x) { this.c = x; return x; }
    }
    static class FakeSerializador implements SerializadorEventosPort {
        @Override public String aJson(Object payload) { return String.valueOf(payload); }
    }

    @BeforeEach
    void setUp() {
        conexion = new ConexionHeadEnd("Landis+Gyr", Protocolo.MQTT);
        outbox = new InMemoryOutbox();
        service = new IntegracionAmiService(new FakeRepo(conexion), outbox, new FakeSerializador());
    }

    private List<String> tiposOutbox() {
        return outbox.todos().stream().map(MensajeOutbox::tipoEvento).toList();
    }

    @Test
    @DisplayName("Solo emite LecturaCrudaRecibida para medidores atendidos (habilitados)")
    void soloEmiteParaMedidoresAtendidos() {
        service.habilitarMedidor("SER-1"); // atendido
        // SER-2 NO habilitado

        int emitidas = service.recibirLote("Landis+Gyr", List.of(
                new LecturaCruda("SER-1", 1500, "Wh", Instant.now()),
                new LecturaCruda("SER-2", 2000, "Wh", Instant.now())
        ));

        assertEquals(1, emitidas);
        assertTrue(tiposOutbox().contains("LecturaCrudaRecibida"));
        assertEquals(1, outbox.todos().size());
    }

    @Test
    @DisplayName("MedidorSuspendido retira el medidor del ACL (deja de emitir)")
    void suspenderDejaDeEmitir() {
        service.habilitarMedidor("SER-9");
        service.suspenderMedidor("SER-9");

        int emitidas = service.recibirLote("Landis+Gyr", List.of(
                new LecturaCruda("SER-9", 1000, "Wh", Instant.now())
        ));
        assertEquals(0, emitidas);
    }

    @Test
    @DisplayName("Reportar degradacion -> estado DEGRADADO y ProveedorDegradado en outbox")
    void degradacionEmiteEvento() {
        ConexionHeadEnd c = service.reportarDegradacion("Timeout del head-end");
        assertEquals(EstadoProveedor.DEGRADADO, c.estado());
        assertTrue(tiposOutbox().contains("ProveedorDegradado"));
    }
}
