package com.voltera.blockchain.domain.model;

import java.time.Instant;
import java.util.List;

/**
 * Lectura (solo lectura) del estado de una propuesta en el contrato
 * {@code MultiFirmaAccionDER}. {@code modo} es ON_CHAIN si se consultó el contrato
 * real o SIMULADO si no hay RPC/dirección configurados (o la lectura falló).
 */
public record LecturaGobernanza(
        String modo,
        boolean existe,
        String estado,
        int aprobaciones,
        List<FirmaLeida> firmas,
        String contrato,
        String red,
        String mensaje
) {
    public static final String ON_CHAIN = "ON_CHAIN";
    public static final String SIMULADO = "SIMULADO";

    /** Firma leída del evento {@code FirmaRegistrada}. */
    public record FirmaLeida(String rol, String firmante, String txHash, Instant firmadoEn) { }

    public static LecturaGobernanza simulada(String red, String mensaje) {
        return new LecturaGobernanza(SIMULADO, false, null, 0, List.of(), null, red, mensaje);
    }
}
