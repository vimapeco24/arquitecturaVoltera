package com.voltera.notificaciones.infrastructure.persistence;

import com.voltera.notificaciones.domain.port.out.InboxPort;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Tabla INBOX de idempotencia por consumidor (lamina 10). Upsert atomico sobre la
 * clave ({@code consumidor::claveIdempotencia}); representa la tabla inbox que en
 * produccion viviria junto al estado del consumidor (misma transaccion).
 */
@Repository
@Profile("!inbox-jdbc")
public class InMemoryInbox implements InboxPort {

    private final ConcurrentHashMap<String, Long> filas = new ConcurrentHashMap<>();
    private final AtomicLong duplicados = new AtomicLong(0);

    @Override
    public boolean registrarSiEsNuevo(String consumidor, String claveIdempotencia, String tipoEvento) {
        final boolean[] esNuevo = {false};
        filas.compute(consumidor + "::" + claveIdempotencia, (k, v) -> {
            if (v == null) { esNuevo[0] = true; return 1L; }
            duplicados.incrementAndGet();
            return v + 1;
        });
        return esNuevo[0];
    }

    @Override
    public long totalUnicos() { return filas.size(); }

    @Override
    public long totalDuplicados() { return duplicados.get(); }
}
