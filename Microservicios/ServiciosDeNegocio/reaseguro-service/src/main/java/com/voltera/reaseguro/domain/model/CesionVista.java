package com.voltera.reaseguro.domain.model;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Modelo de LECTURA (CQRS - read side). Vista inmutable, optimizada para consulta.
 * Se proyecta a partir del agregado de escritura {@link CesionRiesgo}.
 */
public record CesionVista(
        String id,
        String siniestroId,
        String polizaId,
        String prosumidorId,
        BigDecimal montoAprobado,
        BigDecimal montoCedido,
        BigDecimal porcentajeCedido,
        EstadoCesion estado,
        Instant creadaEn
) {

    /**
     * Factory de proyeccion: construye la vista de lectura desde el agregado de escritura.
     */
    public static CesionVista desdeEscritura(CesionRiesgo cesion) {
        return new CesionVista(
                cesion.getId().valor(),
                cesion.getSiniestroId(),
                cesion.getPolizaId(),
                cesion.getProsumidorId(),
                cesion.getMontoAprobado(),
                cesion.getMontoCedido(),
                cesion.getPorcentajeCedido(),
                cesion.getEstado(),
                cesion.getCreadaEn()
        );
    }
}
