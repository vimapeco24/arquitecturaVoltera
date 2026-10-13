package com.voltera.cobros.domain.port.out;

import com.voltera.cobros.domain.model.MensajeInbox;

import java.util.List;

/**
 * Puerto de SALIDA: tabla INBOX de idempotencia por consumidor (BC Cobros).
 */
public interface InboxPort {

    /**
     * @return {@code true} si es la PRIMERA vez (procesar); {@code false} si es
     *         DUPLICADO (ya aplicado).
     */
    boolean registrarSiEsNuevo(String consumidor, String eventId, String tipoEvento);

    List<MensajeInbox> todos();

    long totalUnicos();

    long totalDuplicados();
}
