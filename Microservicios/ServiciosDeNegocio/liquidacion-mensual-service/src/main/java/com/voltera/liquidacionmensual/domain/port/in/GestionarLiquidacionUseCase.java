package com.voltera.liquidacionmensual.domain.port.in;

import com.voltera.liquidacionmensual.domain.model.LiquidacionMensual;

import java.math.BigDecimal;

/**
 * Puerto de ENTRADA: abrir una liquidacion mensual, registrar movimientos
 * (consumos y excedentes) y cerrarla para totalizar.
 */
public interface GestionarLiquidacionUseCase {

    LiquidacionMensual abrir(ComandoAbrir comando);

    LiquidacionMensual actualizar(String liquidacionId, ComandoAbrir comando);

    void eliminar(String liquidacionId);

    LiquidacionMensual registrarMovimiento(ComandoMovimiento comando);

    LiquidacionMensual cerrar(String liquidacionId);

    record ComandoAbrir(String prosumidorId, int anio, int mes,
                        BigDecimal precioConsumoKwh, BigDecimal precioExcedenteKwh) {}

    record ComandoMovimiento(
            String liquidacionId,
            String tipo,          // CONSUMO | EXCEDENTE
            BigDecimal kwh
    ) {}
}
