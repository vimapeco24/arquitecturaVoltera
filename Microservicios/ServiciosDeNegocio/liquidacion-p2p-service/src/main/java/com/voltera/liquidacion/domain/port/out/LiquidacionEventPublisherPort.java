package com.voltera.liquidacion.domain.port.out;

import com.voltera.liquidacion.domain.model.LiquidacionCalculada;

/**
 * Puerto de SALIDA (EDA): publica el evento de liquidación casada en el event
 * mesh. El adaptador por defecto es no-op (no publica); el adaptador Kafka se
 * activa con {@code voltera.kafka.enabled=true}. El dominio/aplicación no
 * dependen de Kafka ni de Spring: solo de este puerto.
 */
public interface LiquidacionEventPublisherPort {

    /**
     * Publica el evento. Devuelve {@code true} si el broker confirmó la
     * recepción; {@code false} si no se pudo publicar (el anclaje REST
     * best-effort sigue sirviendo de respaldo, así que no se pierde el flujo).
     */
    boolean publicar(LiquidacionCalculada evento);
}
