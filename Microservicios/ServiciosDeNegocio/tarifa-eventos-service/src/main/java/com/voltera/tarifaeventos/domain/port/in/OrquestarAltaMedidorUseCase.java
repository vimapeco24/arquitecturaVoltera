package com.voltera.tarifaeventos.domain.port.in;

import com.voltera.tarifaeventos.domain.model.SagaAltaMedidor;

/**
 * Puerto de ENTRADA: process manager (orquestacion/mediador) del alta de medidor
 * (lamina 10).
 *
 * <p>Dirige el alta: inicia la saga publicando {@code MedidorHabilitado} (via outbox)
 * y espera {@code CanalIngestaCreado} y {@code TarifaAsignada}. Si vencen 15 min sin
 * ambas, compensa con {@code HabilitacionFallida} + {@code MedidorSuspendido}.</p>
 */
public interface OrquestarAltaMedidorUseCase {

    /** Inicia el alta: crea la saga y encola MedidorHabilitado en el outbox. */
    SagaAltaMedidor iniciarAlta(String medidorId, String prosumidorId);

    /** Respuesta del servicio de Ingesta. */
    SagaAltaMedidor registrarCanalIngestaCreado(String sagaId);

    /** Respuesta del servicio de Tarifas. */
    SagaAltaMedidor registrarTarifaAsignada(String sagaId);

    /**
     * Barre las sagas pendientes y compensa las que superaron el timeout de 15 min.
     *
     * @return numero de sagas que vencieron y fueron compensadas en esta pasada.
     */
    int procesarTimeouts();

    /**
     * Fuerza el timeout de UNA saga concreta sin esperar los 15 min (demo). Emite
     * las mismas compensaciones (HabilitacionFallida + MedidorSuspendido).
     */
    com.voltera.tarifaeventos.domain.model.SagaAltaMedidor forzarTimeout(String sagaId);
}
