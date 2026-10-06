package com.voltera.habilitacion.domain.port.out;

import com.voltera.habilitacion.domain.model.MensajeOutbox;

import java.util.List;

/** Puerto de SALIDA: tabla OUTBOX transaccional (lamina 10). */
public interface OutboxPort {
    MensajeOutbox agregar(MensajeOutbox mensaje);
    List<MensajeOutbox> pendientes();
    void marcarPublicado(String outboxId);
    List<MensajeOutbox> todos();
}
