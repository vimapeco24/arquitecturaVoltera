package com.voltera.ingesta.domain.port.out;

public interface SerializadorEventosPort {
    String aJson(Object payload);
}
