package com.voltera.notificaciones.domain.port.in;

import com.voltera.notificaciones.domain.event.LecturaSospechosaDetectada;
import com.voltera.notificaciones.domain.event.MedidorHabilitado;
import com.voltera.notificaciones.domain.event.MedidorSinReporte;
import com.voltera.notificaciones.domain.model.Notificacion;

import java.util.List;

/**
 * Puerto de entrada del BC Notificaciones (lamina 02). Reacciona a los eventos de
 * negocio y produce {@code ClienteNotificado}.
 */
public interface NotificarClienteUseCase {

    Notificacion notificarMedidorHabilitado(MedidorHabilitado evento);

    Notificacion notificarLecturaSospechosa(LecturaSospechosaDetectada evento);

    Notificacion notificarMedidorSinReporte(MedidorSinReporte evento);

    List<Notificacion> historial();
}
