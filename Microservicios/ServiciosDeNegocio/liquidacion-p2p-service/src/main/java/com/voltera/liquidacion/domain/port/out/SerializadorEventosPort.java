package com.voltera.liquidacion.domain.port.out;

/** Puerto de SALIDA: serializa el payload de un evento a JSON. */
public interface SerializadorEventosPort {
    String aJson(Object payload);
}
