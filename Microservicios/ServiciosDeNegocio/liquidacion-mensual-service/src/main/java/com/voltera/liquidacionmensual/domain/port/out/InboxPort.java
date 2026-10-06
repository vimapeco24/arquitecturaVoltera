package com.voltera.liquidacionmensual.domain.port.out;

/**
 * Puerto de SALIDA: tabla INBOX de idempotencia por consumidor (lamina 10).
 *
 * <p>Kafka/Redpanda entrega <i>al menos una vez</i>: el mismo evento puede
 * reentregarse tras una reconexion o un rebalanceo del consumer group. Registrar
 * por la clave ({@code consumidor}, {@code claveIdempotencia}) garantiza que un
 * evento repetido no vuelva a producir efecto.</p>
 */
public interface InboxPort {

    /**
     * Registra la observacion del evento.
     *
     * @param consumidor       nombre logico del consumer group.
     * @param claveIdempotencia clave estable del evento: {@code eventId} si viene en
     *                          el payload; si no, {@code tipoEvento|clave|timestamp}.
     * @param tipoEvento       tipo del evento (trazabilidad).
     * @return {@code true} si es la PRIMERA vez (procesar); {@code false} si es DUPLICADO.
     */
    boolean registrarSiEsNuevo(String consumidor, String claveIdempotencia, String tipoEvento);

    long totalUnicos();

    long totalDuplicados();
}
