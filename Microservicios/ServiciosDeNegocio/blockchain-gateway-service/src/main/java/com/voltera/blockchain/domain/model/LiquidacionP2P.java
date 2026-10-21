package com.voltera.blockchain.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

/**
 * Registro canonico de una liquidacion P2P que se va a anclar en la cadena.
 * Contiene SOLO los campos que definen la casacion (gate 8: lo demas vive
 * off-chain). A partir de este registro se computa el hash que se ancla.
 *
 * <p>Inmutable. Se construye a partir del evento de dominio LiquidacionCalculada
 * (o de una peticion REST directa en la PoC).</p>
 */
public final class LiquidacionP2P {

    private final String transaccionId;
    private final String vendedorId;
    private final String compradorId;
    private final long energiaWh;        // energia casada en Wh
    private final BigDecimal precioPorKwh;
    private final BigDecimal valorTotal;
    private final Instant liquidadaEn;
    private final String comunidadId;    // metadato off-chain (NO entra en el hash)

    public LiquidacionP2P(String transaccionId, String vendedorId, String compradorId,
                          long energiaWh, BigDecimal precioPorKwh, BigDecimal valorTotal,
                          Instant liquidadaEn) {
        this(transaccionId, vendedorId, compradorId, energiaWh, precioPorKwh, valorTotal,
                liquidadaEn, null);
    }

    public LiquidacionP2P(String transaccionId, String vendedorId, String compradorId,
                          long energiaWh, BigDecimal precioPorKwh, BigDecimal valorTotal,
                          Instant liquidadaEn, String comunidadId) {
        this.transaccionId = exigir(transaccionId, "transaccionId");
        this.vendedorId = exigir(vendedorId, "vendedorId");
        this.compradorId = exigir(compradorId, "compradorId");
        if (energiaWh <= 0) throw new IllegalArgumentException("energiaWh debe ser > 0");
        this.energiaWh = energiaWh;
        this.precioPorKwh = Objects.requireNonNull(precioPorKwh, "precioPorKwh requerido");
        this.valorTotal = Objects.requireNonNull(valorTotal, "valorTotal requerido");
        this.liquidadaEn = Objects.requireNonNull(liquidadaEn, "liquidadaEn requerido");
        this.comunidadId = (comunidadId == null || comunidadId.isBlank()) ? null : comunidadId.trim();
    }

    private static String exigir(String v, String campo) {
        if (v == null || v.isBlank()) throw new IllegalArgumentException(campo + " requerido");
        return v;
    }

    /**
     * Representacion canonica y determinista del registro, usada para el hash.
     * El orden y formato son fijos para que cualquier parte recompute el mismo hash.
     */
    public String representacionCanonica() {
        return String.join("|",
                "v1",
                transaccionId,
                vendedorId,
                compradorId,
                Long.toString(energiaWh),
                precioPorKwh.toPlainString(),
                valorTotal.toPlainString(),
                Long.toString(liquidadaEn.getEpochSecond()));
    }

    public String transaccionId() { return transaccionId; }
    public String vendedorId() { return vendedorId; }
    public String compradorId() { return compradorId; }
    public long energiaWh() { return energiaWh; }
    public BigDecimal precioPorKwh() { return precioPorKwh; }
    public BigDecimal valorTotal() { return valorTotal; }
    public Instant liquidadaEn() { return liquidadaEn; }
    public String comunidadId() { return comunidadId; }
}
