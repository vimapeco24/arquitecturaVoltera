package com.voltera.liquidacion.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Evento de dominio (EDA): se emite cuando una transacción P2P queda liquidada
 * (casada). Es el eslabón que faltaba para cerrar la orquestación por eventos:
 * el blockchain-gateway lo consume del topic 'topic.facturacion.liquidacion' y
 * ancla el hash en Polygon automáticamente (sin que el frontend tenga que llamar
 * al gateway por REST).
 *
 * <p>El payload usa nombres que el {@code LiquidacionCalculadaListener} del
 * gateway ya entiende: transaccionId, vendedorId, compradorId, energiaWh,
 * precioPorKwh, valorTotal, liquidadaEn, comunidadId.</p>
 *
 * @param eventId        identificador único del evento (idempotencia aguas abajo)
 * @param transaccionId  id de la transacción P2P casada
 * @param vendedorId     prosumidor vendedor
 * @param compradorId    prosumidor comprador
 * @param energiaWh      energía casada en Wh (kWh * 1000; el gateway trabaja en Wh)
 * @param precioPorKwh   precio de casación por kWh
 * @param valorTotal     valor total de la transacción
 * @param liquidadaEn    instante de la liquidación
 * @param comunidadId    comunidad energética (metadato off-chain; puede ser null)
 */
public record LiquidacionCalculada(
        String eventId,
        String transaccionId,
        String vendedorId,
        String compradorId,
        long energiaWh,
        BigDecimal precioPorKwh,
        BigDecimal valorTotal,
        Instant liquidadaEn,
        String comunidadId) {

    /** Construye el evento a partir de una transacción liquidada del dominio. */
    public static LiquidacionCalculada de(TransaccionP2P t, String comunidadId) {
        long energiaWh = t.energia().kwh()
                .multiply(BigDecimal.valueOf(1000))
                .setScale(0, java.math.RoundingMode.HALF_UP)
                .longValueExact();
        return new LiquidacionCalculada(
                UUID.randomUUID().toString(),
                t.id().valor(),
                t.vendedorId(),
                t.compradorId(),
                energiaWh,
                t.precioCasacion().porKwh(),
                t.valorTotal(),
                t.liquidadaEn(),
                comunidadId);
    }
}
