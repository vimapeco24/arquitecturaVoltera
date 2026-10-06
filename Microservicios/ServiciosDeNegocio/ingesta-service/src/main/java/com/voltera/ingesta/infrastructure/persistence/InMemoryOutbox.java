package com.voltera.ingesta.infrastructure.persistence;

import com.voltera.ingesta.domain.model.MensajeOutbox;
import com.voltera.ingesta.domain.port.out.OutboxPort;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryOutbox implements OutboxPort {

    private final ConcurrentHashMap<String, MensajeOutbox> filas = new ConcurrentHashMap<>();

    @Override
    public MensajeOutbox agregar(MensajeOutbox mensaje) {
        filas.put(mensaje.id(), mensaje);
        return mensaje;
    }

    @Override
    public List<MensajeOutbox> pendientes() {
        return filas.values().stream()
                .filter(m -> !m.publicado())
                .sorted(Comparator.comparing(MensajeOutbox::creadoEn))
                .toList();
    }

    @Override
    public void marcarPublicado(String outboxId) {
        filas.computeIfPresent(outboxId, (k, m) -> m.marcarPublicado());
    }

    @Override
    public List<MensajeOutbox> todos() {
        return filas.values().stream()
                .sorted(Comparator.comparing(MensajeOutbox::creadoEn))
                .toList();
    }
}
