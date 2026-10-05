package com.voltera.tarifaeventos.domain.port.out;

/**
 * Puerto de SALIDA: serializa un payload de evento a JSON para el outbox.
 * Mantiene el dominio/aplicacion libre de dependencias de Jackson.
 */
public interface SerializadorEventosPort {
    String aJson(Object payload);
}
