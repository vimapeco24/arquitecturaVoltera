package com.voltera.tarifaeventos.domain.port.out;

import com.voltera.tarifaeventos.domain.model.MensajeOutbox;

import java.util.List;

/**
 * Puerto de SALIDA: tabla OUTBOX transaccional (lamina 10).
 *
 * <p>El process manager anade filas junto con el cambio de estado de la saga. El
 * relay lee las pendientes, las publica al broker y las marca como publicadas.</p>
 */
public interface OutboxPort {

    /** Anade una fila pendiente de publicar. */
    MensajeOutbox agregar(MensajeOutbox mensaje);

    /** Filas aun no publicadas, en orden de creacion. */
    List<MensajeOutbox> pendientes();

    /** Marca una fila como publicada. */
    void marcarPublicado(String outboxId);

    /** Todas las filas (para inspeccion/stats). */
    List<MensajeOutbox> todos();
}
