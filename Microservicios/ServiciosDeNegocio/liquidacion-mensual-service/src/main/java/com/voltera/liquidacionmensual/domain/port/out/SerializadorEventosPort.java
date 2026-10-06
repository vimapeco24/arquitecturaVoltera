package com.voltera.liquidacionmensual.domain.port.out;

public interface SerializadorEventosPort {
    String aJson(Object payload);
}
