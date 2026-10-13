package com.voltera.cobros.infrastructure.persistence;

import com.voltera.cobros.domain.model.MensajeInbox;
import com.voltera.cobros.domain.port.out.InboxPort;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Adaptador en memoria de la tabla INBOX (idempotencia por consumidor).
 *
 * <p>Upsert atómico por ({@code consumidor}, {@code eventId}) con
 * {@link ConcurrentHashMap#compute}. En producción sería una tabla con PK
 * (consumidor, eventId) e {@code INSERT ... ON CONFLICT DO NOTHING}.</p>
 */
@Repository
public class InMemoryInbox implements InboxPort {

    private final ConcurrentHashMap<String, MensajeInbox> filas = new ConcurrentHashMap<>();
    private final AtomicLong duplicados = new AtomicLong(0);

    private static String clave(String consumidor, String eventId) {
        return consumidor + "::" + eventId;
    }

    @Override
    public boolean registrarSiEsNuevo(String consumidor, String eventId, String tipoEvento) {
        final boolean[] esNuevo = {false};
        filas.compute(clave(consumidor, eventId), (k, existente) -> {
            if (existente == null) {
                esNuevo[0] = true;
                return MensajeInbox.nuevo(consumidor, eventId, tipoEvento);
            }
            duplicados.incrementAndGet();
            return existente.vistoDeNuevo();
        });
        return esNuevo[0];
    }

    @Override
    public List<MensajeInbox> todos() {
        return List.copyOf(filas.values());
    }

    @Override
    public long totalUnicos() {
        return filas.size();
    }

    @Override
    public long totalDuplicados() {
        return duplicados.get();
    }
}
