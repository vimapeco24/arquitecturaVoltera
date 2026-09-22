package com.voltera.liquidacion.infrastructure.rest.dto;

import com.voltera.liquidacion.domain.model.TransaccionP2P;

import java.math.BigDecimal;

/**
 * DTO de salida del adaptador REST. Traduce el agregado sin exponer su interior.
 * Incluye el RESULTADO DE LA CASACION (energia solicitada vs casada, pendiente,
 * y si el match fue TOTAL o PARCIAL) para dar informacion de negocio completa.
 */
public record TransaccionResponse(
        String id,
        String ordenVentaId,
        String ordenCompraId,
        String vendedorId,
        String compradorId,
        BigDecimal energiaKwh,
        BigDecimal precioCasacionKwh,
        BigDecimal valorTotal,
        String liquidadaEn,
        // --- Resultado de la casacion ---
        String tipoCasacion,               // TOTAL | PARCIAL
        boolean casacionTotal,
        BigDecimal energiaSolicitadaVentaKwh,
        BigDecimal energiaSolicitadaCompraKwh,
        BigDecimal pendienteVentaKwh,
        BigDecimal pendienteCompraKwh
) {
    public static TransaccionResponse desde(TransaccionP2P t) {
        return new TransaccionResponse(
                t.id().valor(),
                t.ordenVentaId().valor(),
                t.ordenCompraId().valor(),
                t.vendedorId(),
                t.compradorId(),
                t.energia().kwh(),
                t.precioCasacion().porKwh(),
                t.valorTotal(),
                t.liquidadaEn().toString(),
                t.tipoCasacion(),
                t.casacionTotal(),
                t.energiaSolicitadaVenta().kwh(),
                t.energiaSolicitadaCompra().kwh(),
                t.pendienteVenta().kwh(),
                t.pendienteCompra().kwh()
        );
    }
}
