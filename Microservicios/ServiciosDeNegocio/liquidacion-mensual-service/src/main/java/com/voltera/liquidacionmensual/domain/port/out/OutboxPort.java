package com.voltera.liquidacionmensual.domain.port.out;

import com.voltera.liquidacionmensual.domain.model.MensajeOutbox;

import java.util.List;

public interface OutboxPort {
    MensajeOutbox agregar(MensajeOutbox mensaje);
    List<MensajeOutbox> pendientes();
    void marcarPublicado(String outboxId);
    List<MensajeOutbox> todos();
}
