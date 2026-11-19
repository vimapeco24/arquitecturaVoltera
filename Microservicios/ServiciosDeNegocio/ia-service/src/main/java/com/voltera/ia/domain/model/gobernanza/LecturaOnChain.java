package com.voltera.ia.domain.model.gobernanza;

import java.time.Instant;
import java.util.List;

/**
 * Resultado de la lectura (solo lectura) del contrato {@code MultiFirmaAccionDER}
 * vía blockchain-gateway. {@code modo} es {@code ON_CHAIN} cuando el gateway
 * consultó el contrato real, o {@code OFF_CHAIN} cuando no hay contrato/RPC
 * configurados (o el gateway no responde): nunca falla.
 */
public record LecturaOnChain(
        String modo,
        boolean existe,
        String estado,
        int aprobaciones,
        List<FirmaOnChain> firmas,
        String contrato,
        String red,
        String mensaje
) {
    public static final String ON_CHAIN = "ON_CHAIN";
    public static final String OFF_CHAIN = "OFF_CHAIN";

    /** Firma leída de un evento {@code FirmaRegistrada} del contrato. */
    public record FirmaOnChain(String rol, String firmante, String txHash, Instant firmadoEn) { }

    public static LecturaOnChain offChain(String mensaje) {
        return new LecturaOnChain(OFF_CHAIN, false, null, 0, List.of(), null, null, mensaje);
    }

    public boolean esOnChain() {
        return ON_CHAIN.equals(modo);
    }
}
