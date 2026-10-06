package com.voltera.habilitacion.domain.port.in;

import com.voltera.habilitacion.domain.event.OrdenInstalacionCerrada;
import com.voltera.habilitacion.domain.model.Medidor;

/**
 * Puerto de ENTRADA: mediador del alta de medidores (BC Habilitacion, lamina 02).
 */
public interface HabilitarMedidorUseCase {

    /** Dispara el alta desde OrdenInstalacionCerrada: crea el Medidor y emite MedidorHabilitado. */
    Medidor habilitarDesdeOrden(OrdenInstalacionCerrada orden);

    /** Confirmacion de Ingesta. Si ya estan ambas, activa y emite MedidorActivado. */
    Medidor registrarCanalIngestaCreado(String medidorId);

    /** Confirmacion de Tarifas. Si ya estan ambas, activa y emite MedidorActivado. */
    Medidor registrarTarifaAsignada(String medidorId);

    /** Confirmacion de Ingesta recibida por evento (CanalIngestaCreado trae el serial). */
    Medidor confirmarCanalIngestaPorSerial(String serial);

    /** Confirmacion de Tarifas recibida por evento (TarifaAsignada). */
    Medidor confirmarTarifaPorSerialOId(String serialOId);

    /** Barre pendientes y suspende las vencidas (HabilitacionFallida + MedidorSuspendido). */
    int procesarTimeouts();

    /** Fuerza la suspension de un medidor concreto (demo del timeout). */
    Medidor forzarTimeout(String medidorId);
}
