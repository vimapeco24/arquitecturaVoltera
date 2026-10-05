package com.voltera.tarifaeventos.infrastructure.persistence;

import com.voltera.tarifaeventos.domain.model.MensajeInbox;
import com.voltera.tarifaeventos.domain.port.out.InboxPort;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Adaptador en memoria de la tabla INBOX (lamina 10 · idempotencia por consumidor).
 *
 * <p>La clave es ({@code consumidor} + {@code eventId}). El upsert se hace con
 * {@link ConcurrentHashMap#compute} para que sea atomico frente a reentregas
 * concurrentes (varias particiones/replicas). En produccion esta tabla seria una
 * tabla relacional con PRIMARY KEY (consumidor, eventId) y el "upsert" un
 * {@code INSERT ... ON CONFLICT DO NOTHING}, cumpliendo la misma invariante.</p>
 */
@Repository
public class InMemoryInbox implements InboxPort {

    /** key = consumidor + "::" + eventId */
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
