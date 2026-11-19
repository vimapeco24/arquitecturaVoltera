package com.voltera.ia.domain.model.gobernanza;

import java.time.Instant;
import java.util.List;

/**
 * Vista de lectura de las aprobaciones múltiples (3/3) de una propuesta: quién
 * debe firmar, quién ya firmó (con marca de tiempo y tx), quién falta y el estado
 * global. {@code modo} indica si la evidencia es {@code ON_CHAIN} o {@code OFF_CHAIN}.
 */
public record EstadoAprobaciones(
        String propuestaId,
        String activoId,
        String comando,
        String estado,
        String estadoGlobal,
        int firmasRequeridas,
        int firmasAprobadas,
        String progreso,
        String modo,
        String red,
        String contrato,
        String explorerBase,
        Instant creadaEn,
        Instant venceEn,
        List<FirmanteEstado> firmantes,
        List<String> faltantes,
        String txEjecucion,
        String txEjecucionUrl,
        LecturaOnChain lecturaOnChain
) {
    public static final String FIRMO = "FIRMO";
    public static final String PENDIENTE = "PENDIENTE";
    public static final String RECHAZO = "RECHAZO";

    /**
     * Estado de un firmante requerido.
     *
     * @param estado FIRMO | PENDIENTE | RECHAZO
     * @param modo   ON_CHAIN si el txHash es un hash real de transacción; OFF_CHAIN si es referencia demo
     */
    public record FirmanteEstado(
            String firmante,
            String estado,
            String identidad,
            String razon,
            Instant firmadoEn,
            String txHash,
            String explorerUrl,
            String modo
    ) { }
}
