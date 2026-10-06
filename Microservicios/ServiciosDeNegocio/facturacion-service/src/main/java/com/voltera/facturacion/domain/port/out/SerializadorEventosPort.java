package com.voltera.facturacion.domain.port.out;

public interface SerializadorEventosPort {
    String aJson(Object payload);
}
