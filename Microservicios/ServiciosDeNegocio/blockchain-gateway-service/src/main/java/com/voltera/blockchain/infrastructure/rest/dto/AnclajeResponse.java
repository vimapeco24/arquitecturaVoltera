package com.voltera.blockchain.infrastructure.rest.dto;

import com.voltera.blockchain.domain.model.Anclaje;

import java.time.Instant;

/** Vista del estado de un anclaje. */
public record AnclajeResponse(
        String transaccionId,
        String hashCasacion,
        long energiaWh,
        String estado,
        int intentos,
        String txHash,
        String explorerUrl,
        String ultimoError,
        Instant creadoEn,
        Instant actualizadoEn
) {
    public static AnclajeResponse de(Anclaje a, String explorerBase) {
        String url = (a.txHash() == null || explorerBase == null || explorerBase.isBlank())
                ? null
                : explorerBase + "/tx/" + a.txHash();
        return new AnclajeResponse(
                a.transaccionId(), a.hashCasacion(), a.energiaWh(),
                a.estado().name(), a.intentos(), a.txHash(), url,
                a.ultimoError(), a.creadoEn(), a.actualizadoEn());
    }
}
