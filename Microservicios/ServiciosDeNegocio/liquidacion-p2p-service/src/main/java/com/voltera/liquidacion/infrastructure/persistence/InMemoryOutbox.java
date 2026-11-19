package com.voltera.liquidacion.infrastructure.persistence;

import com.voltera.liquidacion.domain.model.MensajeOutbox;
import com.voltera.liquidacion.domain.port.out.OutboxPort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/** Outbox en memoria. Solo existe con {@code voltera.kafka.enabled=true}. */
@Repository
@ConditionalOnProperty(name = "voltera.kafka.enabled", havingValue = "true")
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
