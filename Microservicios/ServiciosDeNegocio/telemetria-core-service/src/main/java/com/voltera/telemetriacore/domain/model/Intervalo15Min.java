package com.voltera.telemetriacore.domain.model;

import java.time.Duration;
import java.time.Instant;

/**
 * Value Object: ventana de agregacion de 15 minutos, alineada al reloj. El inicio
 * se trunca al multiplo de {@code duracionMin} mas cercano hacia abajo.
 */
public record Intervalo15Min(Instant inicio, int duracionMin) {

    public static Intervalo15Min de(Instant momento, int duracionMin) {
        long segInt = (long) duracionMin * 60L;
        long epoch = momento.getEpochSecond();
        long inicio = (epoch / segInt) * segInt;
        return new Intervalo15Min(Instant.ofEpochSecond(inicio), duracionMin);
    }

    public Instant fin() {
        return inicio.plus(Duration.ofMinutes(duracionMin));
    }

    public boolean contiene(Instant momento) {
        return !momento.isBefore(inicio) && momento.isBefore(fin());
    }
}
