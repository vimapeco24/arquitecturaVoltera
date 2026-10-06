package com.voltera.telemetriacore.domain.port.out;

public interface SerializadorEventosPort {
    String aJson(Object payload);
}
