package com.voltera.reaseguro.infrastructure.rest.dto;

import com.voltera.reaseguro.domain.model.CesionVista;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Representacion de salida (lado de lectura) de una cesion de riesgo.
 */
public record CesionVistaResponse(
        String id,
        String siniestroId,
        String polizaId,
        String prosumidorId,
        BigDecimal montoAprobado,
        BigDecimal montoCedido,
        BigDecimal porcentajeCedido,
        String estado,
        Instant creadaEn
) {
    public static CesionVistaResponse desde(CesionVista v) {
        return new CesionVistaResponse(
                v.id(),
                v.siniestroId(),
                v.polizaId(),
                v.prosumidorId(),
                v.montoAprobado(),
                v.montoCedido(),
                v.porcentajeCedido(),
                v.estado().name(),
                v.creadaEn()
        );
    }
}
