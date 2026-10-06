package com.voltera.notificaciones.domain.port.out;

import com.voltera.notificaciones.domain.model.MensajeOutbox;

import java.util.List;

public interface OutboxPort {
    MensajeOutbox agregar(MensajeOutbox mensaje);
    List<MensajeOutbox> pendientes();
    void marcarPublicado(String outboxId);
    List<MensajeOutbox> todos();
}
