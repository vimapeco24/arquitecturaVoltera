package com.voltera.liquidacionmensual.domain.port.in;

/**
 * Puerto de ENTRADA del flujo EDA de Liquidacion Mensual (diagrama DDD 02).
 * Acumula el consumo por intervalo de cada medidor y, al cerrar el periodo
 * (umbral de kWh), emite {@code LiquidacionCalculada}.
 */
public interface AcumularConsumoUseCase {

    /** Acumula un intervalo de consumo neto para el medidor; puede disparar el cierre. */
    void acumular(String medidorSerial, double consumoNetoKwh);
}
