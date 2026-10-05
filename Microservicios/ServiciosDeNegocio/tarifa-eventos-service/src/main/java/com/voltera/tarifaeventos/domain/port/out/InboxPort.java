package com.voltera.tarifaeventos.domain.port.out;

import com.voltera.tarifaeventos.domain.model.MensajeInbox;

import java.util.List;

/**
 * Puerto de SALIDA: tabla INBOX de idempotencia por consumidor (lamina 10).
 *
 * <p>Implementa el <i>upsert</i> por la clave ({@code consumidor}, {@code eventId}):
 * si el evento ya habia sido registrado por ese consumidor, no se vuelve a aplicar
 * (se marca como duplicado); si es la primera vez, se inserta y se autoriza el
 * procesamiento. Es la base de "una lectura repetida tras una reconexion no duplica
 * consumo".</p>
 */
public interface InboxPort {

    /**
     * Registra la recepcion de un evento por un consumidor (upsert atomico).
     *
     * @return {@code true} si es la PRIMERA vez (se debe procesar);
     *         {@code false} si es un DUPLICADO (ya aplicado, no reprocesar).
     */
    boolean registrarSiEsNuevo(String consumidor, String eventId, String tipoEvento);

    /** Devuelve todas las filas del inbox (para inspeccion/stats). */
    List<MensajeInbox> todos();

    /** Numero de eventos unicos aplicados. */
    long totalUnicos();

    /** Numero total de duplicados detectados y descartados. */
    long totalDuplicados();
}
