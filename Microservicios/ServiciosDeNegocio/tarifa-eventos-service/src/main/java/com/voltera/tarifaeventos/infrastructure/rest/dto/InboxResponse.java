package com.voltera.tarifaeventos.infrastructure.rest.dto;

import com.voltera.tarifaeventos.domain.model.MensajeInbox;

import java.util.List;

/**
 * Vista REST de la tabla inbox: estadisticas de idempotencia + filas.
 */
public record InboxResponse(
        long totalUnicos,
        long totalDuplicados,
        List<Fila> filas
) {
    public record Fila(String consumidor, String eventId, String tipoEvento, String primeraVez, long vecesVistas) {}

    public static InboxResponse desde(long totalUnicos, long totalDuplicados, List<MensajeInbox> filas) {
        List<Fila> f = filas.stream()
                .map(m -> new Fila(m.consumidor(), m.eventId(), m.tipoEvento(),
                        m.primeraVez().toString(), m.vecesVistas()))
                .toList();
        return new InboxResponse(totalUnicos, totalDuplicados, f);
    }
}
