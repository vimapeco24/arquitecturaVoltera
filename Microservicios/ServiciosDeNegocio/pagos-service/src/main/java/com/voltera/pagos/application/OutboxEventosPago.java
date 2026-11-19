package com.voltera.pagos.application;

import com.voltera.pagos.domain.model.MensajeOutbox;
import com.voltera.pagos.domain.model.OrdenPago;
import com.voltera.pagos.domain.model.TipoPago;
import com.voltera.pagos.domain.port.out.EventosPagoPort;
import com.voltera.pagos.domain.port.out.OutboxPort;
import com.voltera.pagos.domain.port.out.SerializadorEventosPort;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Registra {@code PagoConfirmado} en el OUTBOX (destino: Cobros, topic
 * {@code topic.cobros.pago-confirmado}). Solo los COBRO_FACTURA confirman un cobro.
 *
 * <p>Formato esperado por cobros-service: eventId, referencia, clienteId, polizaId,
 * montoPagado, confirmadoEn, emitidoEn. Pagos no maneja polizas: se usa la
 * {@code referencia} del pago como {@code polizaId} (clave de conciliacion).</p>
 */
public class OutboxEventosPago implements EventosPagoPort {

    public static final String TIPO_EVENTO = "PagoConfirmado";

    private final OutboxPort outbox;
    private final SerializadorEventosPort serializador;

    public OutboxEventosPago(OutboxPort outbox, SerializadorEventosPort serializador) {
        this.outbox = outbox;
        this.serializador = serializador;
    }

    @Override
    public void pagoConfirmado(OrdenPago orden) {
        if (orden.tipo() != TipoPago.COBRO_FACTURA) return;
        Instant ahora = Instant.now();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("eventId", "evt-" + UUID.randomUUID());
        payload.put("referencia", orden.referencia());
        payload.put("clienteId", orden.prosumidorId());
        payload.put("polizaId", orden.referencia());
        payload.put("montoPagado", orden.monto().valor());
        payload.put("confirmadoEn", ahora.toString());
        payload.put("emitidoEn", ahora.toString());
        outbox.agregar(MensajeOutbox.pendiente(TIPO_EVENTO, orden.referencia(), serializador.aJson(payload)));
    }
}
