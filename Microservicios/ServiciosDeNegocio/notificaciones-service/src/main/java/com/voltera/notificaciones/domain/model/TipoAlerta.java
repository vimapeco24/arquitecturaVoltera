package com.voltera.notificaciones.domain.model;

/**
 * VO: tipo de alerta de negocio que origina la notificacion al cliente.
 * Mapea 1:1 con los eventos que consume el BC Notificaciones (lamina 02).
 */
public enum TipoAlerta {
    /** Origen: MedidorHabilitado (alta confirmada del medidor). */
    MEDIDOR_HABILITADO,
    /** Origen: LecturaSospechosaDetectada (consumo fuera de rango). */
    LECTURA_SOSPECHOSA,
    /** Origen: MedidorSinReporte (el medidor dejo de reportar). */
    MEDIDOR_SIN_REPORTE
}
