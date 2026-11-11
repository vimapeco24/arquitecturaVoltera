package com.voltera.ia.domain.port.out;

import com.voltera.ia.domain.model.Evidencia;
import com.voltera.ia.domain.model.der.AlertaTelemetria;

import java.util.List;

/**
 * Caja 2 · Knowledge Service (RAG). Recupera, para la alerta de un activo DER,
 * las fichas técnicas del fabricante, los históricos de fallos conocidos por
 * modelo de dispositivo y los procedimientos de campo, cada uno con su fuente
 * citable. Esconde el índice vectorial detrás de esta interfaz.
 */
public interface ConocimientoDerPort {

    List<Evidencia> recuperar(AlertaTelemetria alerta);
}
