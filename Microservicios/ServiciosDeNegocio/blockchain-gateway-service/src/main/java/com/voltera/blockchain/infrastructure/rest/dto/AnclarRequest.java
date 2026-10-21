package com.voltera.blockchain.infrastructure.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Peticion para anclar una liquidacion P2P. En produccion este payload llega
 * como evento LiquidacionCalculada desde liquidacion-p2p; el REST es para la PoC
 * y para reanclar manualmente.
 */
public record AnclarRequest(
        @NotBlank String transaccionId,
        @NotBlank String vendedorId,
        @NotBlank String compradorId,
        @Positive long energiaWh,
        @NotNull BigDecimal precioPorKwh,
        @NotNull BigDecimal valorTotal,
        @NotNull Instant liquidadaEn,
        // Comunidad energetica a la que pertenece la casacion (metadato off-chain, opcional).
        String comunidadId
) {}
