package com.voltera.reaseguro.domain.model;

import java.time.Instant;

/**
 * Registro de la <b>tabla INBOX</b> de idempotencia por consumidor.
 *
 * <p>El broker (Kafka/Redpanda) entrega <i>al menos una vez</i>: el mismo evento
 * {@code SiniestroAprobado} puede reentregarse tras una reconexion o un rebalanceo
 * del consumer group {@code reaseguro-consumer}. La tabla inbox guarda, por cada
 * {@code consumidor}, el {@code eventId} ya aplicado; un <b>upsert</b> por la clave
 * ({@code consumidor}, {@code eventId}) garantiza que un evento repetido no vuelva a
 * producir efecto (no duplica la cesion de riesgo).</p>
 *
 * @param consumidor  nombre logico del consumer group (p. ej. {@code reaseguro-consumer})
 * @param eventId     identificador de negocio del evento, generado por el emisor
 * @param tipoEvento  tipo del evento aplicado (trazabilidad)
 * @param primeraVez  instante del primer procesamiento (cuando se inserto la fila)
 * @param vecesVistas numero de veces que el broker entrego este eventId a este consumidor
 */
public record MensajeInbox(
        String consumidor,
        String eventId,
        String tipoEvento,
        Instant primeraVez,
        long vecesVistas
) {
    /** Primera observacion del evento: fila nueva, vista 1. */
    public static MensajeInbox nuevo(String consumidor, String eventId, String tipoEvento) {
        return new MensajeInbox(consumidor, eventId, tipoEvento, Instant.now(), 1L);
    }

    /** Reentrega: misma fila, se incrementa el contador de veces vistas. */
    public MensajeInbox vistoDeNuevo() {
        return new MensajeInbox(consumidor, eventId, tipoEvento, primeraVez, vecesVistas + 1);
    }

    public boolean esDuplicado() {
        return vecesVistas > 1;
    }
}
