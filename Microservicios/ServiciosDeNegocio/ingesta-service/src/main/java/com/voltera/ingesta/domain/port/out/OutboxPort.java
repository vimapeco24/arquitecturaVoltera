package com.voltera.ingesta.domain.port.out;

import com.voltera.ingesta.domain.model.MensajeOutbox;

import java.util.List;

public interface OutboxPort {
    MensajeOutbox agregar(MensajeOutbox mensaje);
    List<MensajeOutbox> pendientes();
    void marcarPublicado(String outboxId);
    List<MensajeOutbox> todos();
}
