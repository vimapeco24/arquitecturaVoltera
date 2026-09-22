package com.voltera.siniestros.domain.port.in;

import com.voltera.siniestros.domain.model.Siniestro;

/**
 * Puerto de ENTRADA: gestiona las transiciones de peritaje y la aprobacion
 * de un siniestro. La aprobacion emite el evento de dominio SiniestroAprobado.
 */
public interface AprobarSiniestroUseCase {

    /** Pasa un siniestro REPORTADO a EN_PERITAJE. */
    Siniestro enviarAPeritaje(String siniestroId);

    /** Aprueba un siniestro EN_PERITAJE y publica el evento SiniestroAprobado. */
    Siniestro aprobar(String siniestroId);

    /** Rechaza un siniestro EN_PERITAJE. */
    Siniestro rechazar(String siniestroId);
}
