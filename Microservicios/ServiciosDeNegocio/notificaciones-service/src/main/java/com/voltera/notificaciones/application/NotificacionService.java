package com.voltera.notificaciones.application;

import com.voltera.notificaciones.domain.event.LecturaSospechosaDetectada;
import com.voltera.notificaciones.domain.event.MedidorHabilitado;
import com.voltera.notificaciones.domain.event.MedidorSinReporte;
import com.voltera.notificaciones.domain.event.FacturaEmitida;
import com.voltera.notificaciones.domain.model.MensajeOutbox;
import com.voltera.notificaciones.domain.model.Notificacion;
import com.voltera.notificaciones.domain.model.Preferencia;
import com.voltera.notificaciones.domain.model.TipoAlerta;
import com.voltera.notificaciones.domain.port.in.NotificarClienteUseCase;
import com.voltera.notificaciones.domain.port.out.NotificacionRepositoryPort;
import com.voltera.notificaciones.domain.port.out.OutboxPort;
import com.voltera.notificaciones.domain.port.out.PreferenciaRepositoryPort;
import com.voltera.notificaciones.domain.port.out.SerializadorEventosPort;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Servicio de aplicacion del BC Notificaciones (lamina 02). Consumidor puro:
 * reacciona a {@code MedidorHabilitado}, {@code LecturaSospechosaDetectada} y
 * {@code MedidorSinReporte}, crea la {@link Notificacion} segun la
 * {@link Preferencia} del cliente y emite {@code ClienteNotificado} por el OUTBOX
 * transaccional. Si el cliente no esta suscrito a ese tipo de alerta, no notifica.
 */
public class NotificacionService implements NotificarClienteUseCase {

    private final NotificacionRepositoryPort repositorio;
    private final PreferenciaRepositoryPort preferencias;
    private final OutboxPort outbox;
    private final SerializadorEventosPort serializador;

    public NotificacionService(NotificacionRepositoryPort repositorio,
                               PreferenciaRepositoryPort preferencias,
                               OutboxPort outbox,
                               SerializadorEventosPort serializador) {
        this.repositorio = repositorio;
        this.preferencias = preferencias;
        this.outbox = outbox;
        this.serializador = serializador;
    }

    @Override
    public Notificacion notificarMedidorHabilitado(MedidorHabilitado evento) {
        String medidorId = primero(evento.serial(), evento.medidorId());
        return notificar(medidorId, TipoAlerta.MEDIDOR_HABILITADO, null);
    }

    @Override
    public Notificacion notificarLecturaSospechosa(LecturaSospechosaDetectada evento) {
        String mensaje = "Consumo inusual (" + evento.consumoKwh() + " kWh) en su medidor "
                + evento.medidorSerial() + ". " + (evento.motivo() != null ? evento.motivo() : "");
        return notificar(evento.medidorSerial(), TipoAlerta.LECTURA_SOSPECHOSA, mensaje.trim());
    }

    @Override
    public Notificacion notificarMedidorSinReporte(MedidorSinReporte evento) {
        String mensaje = "Su medidor " + evento.medidorSerial()
                + " dejo de reportar consumo (ultima lectura: " + evento.ultimaLecturaEn() + ").";
        return notificar(evento.medidorSerial(), TipoAlerta.MEDIDOR_SIN_REPORTE, mensaje);
    }

    @Override
    public Notificacion notificarFacturaEmitida(FacturaEmitida evento) {
        String mensaje = "Se emitio su factura " + evento.facturaId() + " del periodo "
                + evento.periodo() + " por un monto de " + evento.monto() + ".";
        return notificar(evento.medidorSerial(), TipoAlerta.FACTURA_EMITIDA, mensaje);
    }

    @Override
    public List<Notificacion> historial() {
        return repositorio.todas();
    }

    /**
     * Nucleo de la reaccion: resuelve preferencia, respeta el opt-in, crea la
     * notificacion, la persiste y encola {@code ClienteNotificado}.
     * Devuelve {@code null} si el cliente no esta suscrito a esa alerta.
     */
    private Notificacion notificar(String medidorId, TipoAlerta tipo, String mensaje) {
        if (medidorId == null || medidorId.isBlank()) {
            throw new IllegalArgumentException("No se puede notificar sin identificador de medidor");
        }
        Preferencia preferencia = preferencias.preferenciaDe(medidorId);
        if (!preferencia.aceptaAlerta(tipo)) {
            return null; // el cliente opto por no recibir este tipo de alerta
        }
        Notificacion notificacion = Notificacion.crear(medidorId, tipo, mensaje, preferencia);
        repositorio.guardar(notificacion);
        emitirClienteNotificado(notificacion);
        return notificacion;
    }

    private void emitirClienteNotificado(Notificacion n) {
        Map<String, String> payload = new LinkedHashMap<>();
        payload.put("notificacionId", n.notificacionId());
        payload.put("medidorId", n.medidorId());
        payload.put("tipoAlerta", n.tipo().name());
        payload.put("canal", n.canal().name());
        payload.put("mensaje", n.mensaje());
        payload.put("generadaEn", n.generadaEn().toString());
        outbox.agregar(MensajeOutbox.pendiente(
                "ClienteNotificado", n.medidorId(), serializador.aJson(payload)));
    }

    private static String primero(String a, String b) {
        if (a != null && !a.isBlank()) return a;
        return b;
    }
}
