package com.voltera.ia.domain.model.der;

/**
 * Tipo de activo de energía distribuida (DER) que gestiona Voltera: paneles
 * solares, baterías, inversores y cargadores de vehículo eléctrico. El tipo
 * condiciona qué anomalías son plausibles y qué fichas técnicas recupera el RAG.
 */
public enum TipoActivo {
    PANEL_SOLAR,
    BATERIA,
    INVERSOR,
    CARGADOR_EV,
    MEDIDOR
}
