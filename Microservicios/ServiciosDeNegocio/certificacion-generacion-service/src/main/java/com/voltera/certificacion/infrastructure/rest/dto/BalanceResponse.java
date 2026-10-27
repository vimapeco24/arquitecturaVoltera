package com.voltera.certificacion.infrastructure.rest.dto;

import com.voltera.certificacion.domain.model.BalanceGeneracionCertificable;

/** Vista REST del balance de generación certificable de un medidor. */
public record BalanceResponse(
        String medidorId,
        long bloqueWh,
        long generacionAcumuladaWh,
        long bloquesReservados,
        int medicionesContabilizadas,
        boolean puedeReservarBloque) {

    public static BalanceResponse desde(BalanceGeneracionCertificable b) {
        return new BalanceResponse(
                b.medidorId(), b.bloqueWh(), b.generacionAcumuladaWh(), b.bloquesReservados(),
                b.medicionesContabilizadas(), b.puedeReservarBloque());
    }
}
