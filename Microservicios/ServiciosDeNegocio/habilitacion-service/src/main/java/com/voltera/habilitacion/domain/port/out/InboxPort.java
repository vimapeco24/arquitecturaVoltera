package com.voltera.habilitacion.domain.port.out;

/**
 * Puerto de SALIDA: tabla INBOX de idempotencia por consumidor (lamina 10). El
 * broker entrega al-menos-una-vez; registrar por (consumidor, eventId) evita
 * procesar dos veces la misma OrdenInstalacionCerrada.
 */
public interface InboxPort {
    /** @return true si es la PRIMERA vez (procesar); false si es DUPLICADO. */
    boolean registrarSiEsNuevo(String consumidor, String eventId, String tipoEvento);
    long totalUnicos();
    long totalDuplicados();
}
