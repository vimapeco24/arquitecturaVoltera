package com.voltera.tarifas.domain.port.out;

import com.voltera.tarifas.domain.model.MensajeOutbox;

import java.util.List;

public interface OutboxPort {
    MensajeOutbox agregar(MensajeOutbox mensaje);
    List<MensajeOutbox> pendientes();
    void marcarPublicado(String outboxId);
    List<MensajeOutbox> todos();
}
