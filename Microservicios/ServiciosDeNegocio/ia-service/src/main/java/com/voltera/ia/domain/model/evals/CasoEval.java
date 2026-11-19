package com.voltera.ia.domain.model.evals;

import com.voltera.ia.domain.model.der.Severidad;
import com.voltera.ia.domain.model.der.TipoAccion;
import com.voltera.ia.domain.model.der.TipoActivo;

import java.math.BigDecimal;

/**
 * Caso etiquetado del dataset de Evals: una alerta y la respuesta correcta del
 * dominio (según las fichas y fallos conocidos del corpus), no la salida del motor.
 */
public record CasoEval(
        String id,
        String descripcion,
        TipoActivo tipoActivo,
        String metrica,
        BigDecimal valor,
        BigDecimal umbral,
        int minutosSinSenal,
        Severidad severidadEsperada,
        TipoAccion accionEsperada,
        boolean requiereHumanoEsperado,
        String nota
) {}
