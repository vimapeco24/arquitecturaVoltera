package com.voltera.telemetria.domain.port.out;

import com.voltera.telemetria.domain.model.ConsumoRegistrado;

/**
 * Puerto de SALIDA (EDA): publica eventos de dominio hacia el broker
 * (Kafka/Redpanda). Implementado en infraestructura por un adaptador basado en
 * KafkaTemplate.
 *
 * <p>Devuelve {@code true} si el evento se entrego al broker y {@code false} si
 * el broker no estaba disponible: eso permite al servicio de aplicacion decidir
 * si debe almacenar el evento en el buffer offline (store-and-forward).</p>
 */
public interface EventPublisherPort {

    /** @return true si el broker acepto el evento; false si no hay conexion. */
    boolean publicar(ConsumoRegistrado evento);
}
