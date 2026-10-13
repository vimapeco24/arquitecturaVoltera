package com.voltera.reaseguro.domain.port.in;

import com.voltera.reaseguro.domain.model.CesionVista;
import com.voltera.reaseguro.domain.model.MensajeInbox;

import java.util.List;

/**
 * Puerto de entrada del lado de LECTURA (consulta CQRS). Devuelve vistas
 * inmutables {@link CesionVista} desde el modelo de lectura, y expone el estado
 * de la tabla INBOX de idempotencia (unicos / duplicados) para inspeccion.
 */
public interface ConsultarCesionUseCase {

    CesionVista porId(String id);

    List<CesionVista> porPoliza(String polizaId);

    List<CesionVista> todas();

    /** Filas de la tabla INBOX de idempotencia (eventId por consumidor). */
    List<MensajeInbox> inbox();

    /** Numero de eventos unicos aplicados por el consumidor. */
    long inboxUnicos();

    /** Numero de reentregas duplicadas descartadas por idempotencia. */
    long inboxDuplicados();
}
