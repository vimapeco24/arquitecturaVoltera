package com.voltera.ingesta.domain.model;

/**
 * Resultado de procesar una lectura cruda en la sesion de ingesta.
 */
public enum ResultadoValidacion {
    /** Lectura valida y no duplicada: se emite LecturaValidada. */
    VALIDADA,
    /** Fuera de rango: se emite LecturaSospechosaDetectada. */
    SOSPECHOSA,
    /** Ya vista en la ventana de duplicados: se descarta (idempotencia). */
    DUPLICADA,
    /** El medidor no esta habilitado para esta sesion: se ignora. */
    NO_HABILITADO
}
