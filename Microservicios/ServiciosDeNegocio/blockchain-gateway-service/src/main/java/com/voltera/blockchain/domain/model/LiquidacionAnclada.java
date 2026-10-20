package com.voltera.blockchain.domain.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Evento de dominio que el gateway publica en Kafka/Redpanda cuando una
 * liquidacion P2P queda ANCLADA y confirmada en la cadena. Es la "confirmacion
 * por evento" del gate 6: el resto del sistema (facturacion, auditoria) se entera
 * de forma asincrona, sin esperar a la cadena en el camino critico.
 *
 * @param eventId       id unico del evento (idempotencia aguas abajo)
 * @param transaccionId transaccion P2P anclada
 * @param hashCasacion  hash anclado on-chain
 * @param txHash        hash de la transaccion en Polygon (trazabilidad en el explorer)
 * @param energiaWh     energia casada (Wh)
 * @param red           red destino (p. ej. "polygon-amoy")
 * @param ocurridoEn    marca temporal
 */
public record LiquidacionAnclada(
        String eventId,
        String transaccionId,
        String hashCasacion,
        String txHash,
        long energiaWh,
        String red,
        Instant ocurridoEn
) {
    public static LiquidacionAnclada de(Anclaje a, String red) {
        return new LiquidacionAnclada(
                UUID.randomUUID().toString(),
                a.transaccionId(),
                a.hashCasacion(),
                a.txHash(),
                a.energiaWh(),
                red,
                Instant.now());
    }
}
