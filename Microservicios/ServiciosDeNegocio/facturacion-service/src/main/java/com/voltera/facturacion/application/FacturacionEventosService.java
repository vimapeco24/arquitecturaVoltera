package com.voltera.facturacion.application;

import com.voltera.facturacion.domain.model.MensajeOutbox;
import com.voltera.facturacion.domain.port.in.FacturarDesdeLiquidacionUseCase;
import com.voltera.facturacion.domain.port.out.OutboxPort;
import com.voltera.facturacion.domain.port.out.SerializadorEventosPort;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Servicio de aplicacion EDA de Facturacion (diagrama DDD 02).
 *
 * <p>Reacciona a {@code LiquidacionCalculada} (de Liquidacion Mensual): calcula el
 * monto de la factura con el precio por kWh configurado y publica
 * {@code FacturaEmitida} por el OUTBOX (destino: Notificaciones). Complementa el
 * {@code FacturacionService} REST existente sin modificarlo.</p>
 */
public class FacturacionEventosService implements FacturarDesdeLiquidacionUseCase {

    private final OutboxPort outbox;
    private final SerializadorEventosPort serializador;
    private final double precioKwh;

    public FacturacionEventosService(OutboxPort outbox, SerializadorEventosPort serializador,
                                     double precioKwh) {
        this.outbox = outbox;
        this.serializador = serializador;
        this.precioKwh = precioKwh;
    }

    @Override
    public void facturarDesdeLiquidacion(String liquidacionId, String medidorSerial,
                                         String periodo, double consumoTotalKwh) {
        if (medidorSerial == null || medidorSerial.isBlank()) return;
        double monto = consumoTotalKwh * precioKwh;
        String facturaId = "FAC-" + UUID.randomUUID();

        Map<String, String> payload = new LinkedHashMap<>();
        payload.put("facturaId", facturaId);
        payload.put("liquidacionId", String.valueOf(liquidacionId));
        payload.put("medidorSerial", medidorSerial);
        payload.put("periodo", String.valueOf(periodo));
        payload.put("consumoTotalKwh", String.valueOf(consumoTotalKwh));
        payload.put("precioKwh", String.valueOf(precioKwh));
        payload.put("monto", String.valueOf(monto));
        payload.put("emitidaEn", Instant.now().toString());
        outbox.agregar(MensajeOutbox.pendiente("FacturaEmitida", medidorSerial, serializador.aJson(payload)));
    }
}
