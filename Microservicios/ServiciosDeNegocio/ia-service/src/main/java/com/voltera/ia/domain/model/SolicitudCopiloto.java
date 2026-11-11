package com.voltera.ia.domain.model;

import java.math.BigDecimal;

/**
 * Entrada al Copiloto de Siniestros: el contexto minimo del caso que el agente
 * va a razonar. Puede venir de un disparador por evento (SiniestroAprobado /
 * SiniestroReportado) o de una consulta manual del analista desde la consola.
 *
 * @param siniestroId     id del siniestro (si existe en el dominio).
 * @param polizaId        poliza asociada.
 * @param prosumidorId    prosumidor/asegurado.
 * @param descripcion     descripcion libre del caso (texto que el analista ve).
 * @param montoReclamacion monto reclamado en COP (decide el umbral human-in-the-loop).
 * @param disparadoPorEvento true si el flujo lo despertó un evento del broker.
 */
public record SolicitudCopiloto(
        String siniestroId,
        String polizaId,
        String prosumidorId,
        String descripcion,
        BigDecimal montoReclamacion,
        boolean disparadoPorEvento
) {
    public SolicitudCopiloto {
        if (montoReclamacion == null) {
            montoReclamacion = BigDecimal.ZERO;
        }
        if (descripcion == null) {
            descripcion = "";
        }
    }
}
