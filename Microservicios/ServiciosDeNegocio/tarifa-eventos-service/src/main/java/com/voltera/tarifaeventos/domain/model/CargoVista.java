package com.voltera.tarifaeventos.domain.model;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Modelo de LECTURA (CQRS - read side). Vista inmutable, optimizada para
 * consulta. Se proyecta a partir del agregado de escritura {@link CargoTarifa}.
 */
public record CargoVista(
        String id,
        String medidorId,
        String prosumidorId,
        BigDecimal consumoKwh,
        BigDecimal umbralKwh,
        BigDecimal excedenteKwh,
        BigDecimal precioExtraKwh,
        BigDecimal montoCargo,
        EstadoCargo estado,
        Instant creadoEn
) {

    /** Factory de proyeccion: construye la vista de lectura desde el agregado de escritura. */
    public static CargoVista desdeEscritura(CargoTarifa cargo) {
        return new CargoVista(
                cargo.getId().valor(),
                cargo.getMedidorId(),
                cargo.getProsumidorId(),
                cargo.getConsumoKwh(),
                cargo.getUmbralKwh(),
                cargo.getExcedenteKwh(),
                cargo.getPrecioExtraKwh(),
                cargo.getMontoCargo(),
                cargo.getEstado(),
                cargo.getCreadoEn()
        );
    }
}
