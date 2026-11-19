package com.voltera.liquidacion.domain.port.out;

import com.voltera.liquidacion.domain.model.MensajeOutbox;

import java.util.List;

/** Puerto de SALIDA: almacén del patrón Outbox. */
public interface OutboxPort {
    MensajeOutbox agregar(MensajeOutbox mensaje);

    List<MensajeOutbox> pendientes();

    void marcarPublicado(String outboxId);

    List<MensajeOutbox> todos();
}
