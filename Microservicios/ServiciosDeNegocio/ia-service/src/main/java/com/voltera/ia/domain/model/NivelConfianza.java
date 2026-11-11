package com.voltera.ia.domain.model;

/**
 * Nivel de confianza de la recomendacion del copiloto. Determina, junto con el
 * monto, si la propuesta puede resolverse dentro de los limites del agente o si
 * exige revision humana (human-in-the-loop).
 */
public enum NivelConfianza {
    ALTA,
    MEDIA,
    BAJA
}
