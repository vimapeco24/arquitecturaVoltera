package com.voltera.telemetria.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Evento de dominio: se emite cuando un medidor ACTIVO registra una lectura de
 * consumo. El {@code eventId} (UUID) es la clave de idempotencia para el
 * consumidor (tarifa-eventos-service).
 *
 * <p>El evento sigue el patron de <b>transferencia de estado</b>: transporta
 * todo lo necesario (consumoKwh, prosumidorId, si es consumo extra, la marca de
 * tiempo del evento) para que el consumidor calcule el cargo sin volver a
 * consultar a telemetria-service.</p>
 *
 * <p>{@code consumoExtra} indica que la lectura supero el umbral contratado del
 * medidor: es la senal que dispara un cargo tarifario adicional en el proveedor.</p>
 */
public record ConsumoRegistrado(
        UUID eventId,
        String medidorId,
        String prosumidorId,
        BigDecimal consumoKwh,
        BigDecimal umbralKwh,
        boolean consumoExtra,
        Instant ocurridoEn,
        Instant emitidoEn
) {
    public static ConsumoRegistrado desde(Medidor medidor, LecturaConsumo lectura, boolean consumoExtra) {
        return new ConsumoRegistrado(
                UUID.randomUUID(),
                medidor.id().valor(),
                medidor.prosumidorId(),
                lectura.consumo().valor(),
                medidor.umbralKwh().valor(),
                consumoExtra,
                lectura.capturadaEn(),
                Instant.now()
        );
    }
}
