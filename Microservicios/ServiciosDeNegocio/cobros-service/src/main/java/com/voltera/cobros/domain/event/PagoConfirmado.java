package com.voltera.cobros.domain.event;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Evento de integración consumido desde el topic {@code topic.cobros.pago-confirmado}
 * (emitido por el contexto de Pagos). Transporta el estado necesario para conciliar
 * el movimiento sin consultar al servicio origen (transferencia de estado).
 *
 * <p>El {@code eventId} es la clave de idempotencia del consumidor; la
 * {@code referencia} identifica el pago para la conciliación.</p>
 */
public record PagoConfirmado(
        String eventId,
        String referencia,
        String clienteId,
        String polizaId,
        BigDecimal montoPagado,
        Instant confirmadoEn,
        Instant emitidoEn
) {
}
