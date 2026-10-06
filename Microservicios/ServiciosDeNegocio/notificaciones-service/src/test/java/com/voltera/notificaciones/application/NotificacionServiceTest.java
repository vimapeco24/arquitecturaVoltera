package com.voltera.notificaciones.application;

import com.voltera.notificaciones.domain.event.LecturaSospechosaDetectada;
import com.voltera.notificaciones.domain.event.MedidorHabilitado;
import com.voltera.notificaciones.domain.event.MedidorSinReporte;
import com.voltera.notificaciones.domain.model.CanalNotificacion;
import com.voltera.notificaciones.domain.model.MensajeOutbox;
import com.voltera.notificaciones.domain.model.Notificacion;
import com.voltera.notificaciones.domain.model.Preferencia;
import com.voltera.notificaciones.domain.model.TipoAlerta;
import com.voltera.notificaciones.domain.port.out.SerializadorEventosPort;
import com.voltera.notificaciones.infrastructure.persistence.InMemoryNotificacionRepository;
import com.voltera.notificaciones.infrastructure.persistence.InMemoryOutbox;
import com.voltera.notificaciones.infrastructure.persistence.InMemoryPreferenciaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("NotificacionService - consumidor EDA, reacciona a eventos y emite ClienteNotificado (lamina 02)")
class NotificacionServiceTest {

    private InMemoryNotificacionRepository repositorio;
    private InMemoryPreferenciaRepository preferencias;
    private InMemoryOutbox outbox;
    private NotificacionService service;

    static class FakeSerializador implements SerializadorEventosPort {
        @Override public String aJson(Object payload) { return String.valueOf(payload); }
    }

    @BeforeEach
    void setUp() {
        repositorio = new InMemoryNotificacionRepository();
        preferencias = new InMemoryPreferenciaRepository();
        outbox = new InMemoryOutbox();
        service = new NotificacionService(repositorio, preferencias, outbox, new FakeSerializador());
    }

    private List<String> tiposOutbox() {
        return outbox.todos().stream().map(MensajeOutbox::tipoEvento).toList();
    }

    @Test
    @DisplayName("MedidorHabilitado genera notificacion y emite ClienteNotificado por el outbox")
    void medidorHabilitadoNotifica() {
        Notificacion n = service.notificarMedidorHabilitado(
                new MedidorHabilitado("MED-1", "SER-1", "ACTIVADO"));

        assertNotNull(n);
        assertEquals(TipoAlerta.MEDIDOR_HABILITADO, n.tipo());
        assertEquals("SER-1", n.medidorId()); // usa el serial como identificador si viene
        assertEquals(CanalNotificacion.EMAIL, n.canal()); // preferencia por defecto
        assertEquals(1, service.historial().size());
        assertTrue(tiposOutbox().contains("ClienteNotificado"));
    }

    @Test
    @DisplayName("LecturaSospechosaDetectada emite ClienteNotificado con el motivo en el mensaje")
    void lecturaSospechosaNotifica() {
        Notificacion n = service.notificarLecturaSospechosa(
                new LecturaSospechosaDetectada("SER-2", 999.0, "Fuera del rango [0, 100] kWh"));

        assertNotNull(n);
        assertEquals(TipoAlerta.LECTURA_SOSPECHOSA, n.tipo());
        assertTrue(n.mensaje().contains("SER-2"));
        assertTrue(n.mensaje().contains("Fuera del rango"));
        assertEquals(1, tiposOutbox().size());
        assertEquals("ClienteNotificado", tiposOutbox().get(0));
    }

    @Test
    @DisplayName("MedidorSinReporte emite ClienteNotificado")
    void medidorSinReporteNotifica() {
        Notificacion n = service.notificarMedidorSinReporte(
                new MedidorSinReporte("SER-3", "2026-01-01T10:00:00Z"));

        assertNotNull(n);
        assertEquals(TipoAlerta.MEDIDOR_SIN_REPORTE, n.tipo());
        assertTrue(tiposOutbox().contains("ClienteNotificado"));
    }

    @Test
    @DisplayName("Respeta el opt-in: si el cliente no esta suscrito al tipo de alerta, no notifica ni emite evento")
    void respetaOptIn() {
        // Preferencia que solo acepta MEDIDOR_HABILITADO (no LECTURA_SOSPECHOSA)
        preferencias.registrar("SER-4",
                new Preferencia(CanalNotificacion.SMS, EnumSet.of(TipoAlerta.MEDIDOR_HABILITADO)));

        Notificacion n = service.notificarLecturaSospechosa(
                new LecturaSospechosaDetectada("SER-4", 50.0, "motivo"));

        assertNull(n);
        assertTrue(service.historial().isEmpty());
        assertTrue(outbox.todos().isEmpty());
    }

    @Test
    @DisplayName("Usa el canal de la preferencia registrada del cliente")
    void usaCanalDePreferencia() {
        preferencias.registrar("SER-5",
                new Preferencia(CanalNotificacion.PUSH, EnumSet.allOf(TipoAlerta.class)));

        Notificacion n = service.notificarMedidorSinReporte(
                new MedidorSinReporte("SER-5", "2026-01-01T10:00:00Z"));

        assertNotNull(n);
        assertEquals(CanalNotificacion.PUSH, n.canal());
    }

    @Test
    @DisplayName("Notificar sin identificador de medidor lanza IllegalArgumentException")
    void sinIdentificadorFalla() {
        assertThrows(IllegalArgumentException.class, () ->
                service.notificarMedidorHabilitado(new MedidorHabilitado(null, null, "ACTIVADO")));
    }
}
