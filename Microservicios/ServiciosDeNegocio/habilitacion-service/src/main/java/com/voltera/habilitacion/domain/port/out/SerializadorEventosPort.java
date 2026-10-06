package com.voltera.habilitacion.domain.port.out;

/** Puerto de SALIDA: serializa payloads de evento a JSON para el outbox. */
public interface SerializadorEventosPort {
    String aJson(Object payload);
}
