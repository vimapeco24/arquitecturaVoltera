package com.voltera.telemetriacore.domain.model;

import java.time.Instant;

/**
 * Vista <b>agregada</b> del lado QUERY (CQRS, lámina 03: "Vista de lectura · Redis
 * (en vivo) + vistas agregadas").
 *
 * <p>Mantiene el acumulado por medidor: consumo total (kWh), número de intervalos
 * registrados y marca del último intervalo proyectado. El Proyector la actualiza de
 * forma incremental cada vez que se cierra un intervalo, de modo que la API de
 * lectura responde el total sin recalcular sobre la serie append-only.</p>
 */
public record ConsumoAgregadoVista(
        String medidorSerial,
        double consumoTotalKwh,
        long intervalosRegistrados,
        Instant ultimoIntervaloFin,
        Instant actualizadoEn
) {
    /** Primer agregado para un medidor que aún no tenía vista. */
    public static ConsumoAgregadoVista inicial(String medidorSerial) {
        return new ConsumoAgregadoVista(medidorSerial, 0.0, 0, null, Instant.now());
    }

    /** Devuelve una NUEVA vista con el intervalo recién cerrado sumado (inmutable). */
    public ConsumoAgregadoVista acumular(SerieDeMedicion.ConsumoNetoIntervalo neto) {
        return new ConsumoAgregadoVista(
                medidorSerial,
                consumoTotalKwh + neto.consumoNetoKwh(),
                intervalosRegistrados + 1,
                neto.fin(),
                Instant.now());
    }
}
