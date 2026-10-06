package com.voltera.notificaciones.domain.port.out;

import com.voltera.notificaciones.domain.model.Notificacion;

import java.util.List;

public interface NotificacionRepositoryPort {
    Notificacion guardar(Notificacion notificacion);
    List<Notificacion> todas();
}
