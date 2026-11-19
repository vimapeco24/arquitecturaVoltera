package com.voltera.pagos.domain.port.out;

public interface SerializadorEventosPort {
    String aJson(Object payload);
}
