package com.voltera.tarifas.domain.port.in;

/**
 * Puerto de ENTRADA del flujo EDA del BC Tarifas (diagrama DDD 02).
 * Reacciona a eventos: asigna tarifa al habilitarse un medidor y registra el
 * consumo tarificado por intervalo.
 */
public interface AsignarTarifaUseCase {

    /** Ante MedidorHabilitado: asigna una tarifa vigente al medidor y emite TarifaAsignada. */
    void asignarTarifaAMedidor(String medidorSerial);

    /** Ante ConsumoIntervaloRegistrado: tarifica el consumo del intervalo (precio vigente). */
    void registrarConsumoTarificado(String medidorSerial, double consumoNetoKwh, int hora);
}
