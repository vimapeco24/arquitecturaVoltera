package com.voltera.notificaciones.domain.port.out;

public interface SerializadorEventosPort {
    String aJson(Object payload);
}
