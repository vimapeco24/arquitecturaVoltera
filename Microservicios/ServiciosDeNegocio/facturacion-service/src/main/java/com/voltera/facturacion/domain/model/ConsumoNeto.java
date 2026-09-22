package com.voltera.facturacion.domain.model;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Value Object: consumo neto del periodo (energia consumida de la red
 * menos energia inyectada). Puede ser negativo si el prosumidor genero
 * mas de lo que consumio.
 */
public final class ConsumoNeto {

    private final BigDecimal kwhConsumidos;
    private final BigDecimal kwhInyectados;

    private ConsumoNeto(BigDecimal kwhConsumidos, BigDecimal kwhInyectados) {
        if (kwhConsumidos.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Los kWh consumidos no pueden ser negativos");
        }
        if (kwhInyectados.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Los kWh inyectados no pueden ser negativos");
        }
        this.kwhConsumidos = kwhConsumidos;
        this.kwhInyectados = kwhInyectados;
    }

    public static ConsumoNeto de(BigDecimal kwhConsumidos, BigDecimal kwhInyectados) {
        Objects.requireNonNull(kwhConsumidos, "kwhConsumidos requerido");
        Objects.requireNonNull(kwhInyectados, "kwhInyectados requerido");
        return new ConsumoNeto(kwhConsumidos, kwhInyectados);
    }

    /** Consumo neto = consumido - inyectado. Negativo => excedente. */
    public BigDecimal neto() {
        return kwhConsumidos.subtract(kwhInyectados);
    }

    /** kWh de excedente disponibles para venta (0 si consumio mas de lo que genero). */
    public BigDecimal excedente() {
        BigDecimal neto = neto();
        return neto.compareTo(BigDecimal.ZERO) < 0 ? neto.abs() : BigDecimal.ZERO;
    }

    /** kWh que debe pagar a la red (0 si genero mas de lo que consumio). */
    public BigDecimal consumoFacturable() {
        BigDecimal neto = neto();
        return neto.compareTo(BigDecimal.ZERO) > 0 ? neto : BigDecimal.ZERO;
    }

    public BigDecimal kwhConsumidos() {
        return kwhConsumidos;
    }

    public BigDecimal kwhInyectados() {
        return kwhInyectados;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ConsumoNeto c)) return false;
        return kwhConsumidos.compareTo(c.kwhConsumidos) == 0
                && kwhInyectados.compareTo(c.kwhInyectados) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(kwhConsumidos.stripTrailingZeros(), kwhInyectados.stripTrailingZeros());
    }
}
