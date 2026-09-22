package com.voltera.telemetria.domain.port.in;

import com.voltera.telemetria.domain.model.Medidor;

import java.math.BigDecimal;

/**
 * Puerto de ENTRADA: gestiona el alta y el ciclo de vida de un medidor.
 * La activacion es la <b>transferencia de estado</b> del negocio: al activar un
 * medidor se dispara la generacion de la notificacion liquidada (factura,
 * tarifas) en el consumidor.
 */
public interface GestionarMedidorUseCase {

    Medidor registrar(ComandoRegistrarMedidor comando);

    /** Activa el medidor (INACTIVO/SUSPENDIDO -> ACTIVO). */
    Medidor activar(String medidorId);

    /** Suspende el medidor (ACTIVO -> SUSPENDIDO). */
    Medidor suspender(String medidorId);

    void eliminar(String medidorId);

    record ComandoRegistrarMedidor(
            String prosumidorId,
            BigDecimal umbralKwh
    ) {}
}
