package com.voltera.tarifaeventos.domain.port.in;

import com.voltera.tarifaeventos.domain.model.NotificacionLiquidada;

import java.math.BigDecimal;

/**
 * Puerto de ENTRADA: al activar un nuevo medidor/prosumidor, genera la
 * notificacion liquidada (factura, tarifas) y la publica en el topico de
 * notificacion. Cumple el requisito de "transferencia de estado" del negocio.
 */
public interface ActivarMedidorUseCase {

    NotificacionLiquidada activar(ComandoActivarMedidor comando);

    record ComandoActivarMedidor(
            String medidorId,
            String prosumidorId,
            BigDecimal umbralKwh
    ) {}
}
