package com.voltera.ia.domain.port.out;

import com.voltera.ia.domain.model.Evidencia;
import com.voltera.ia.domain.model.SolicitudCopiloto;

import java.util.List;

/**
 * Puerto de salida hacia el Knowledge Service (RAG): recupera fragmentos de
 * conocimiento relevantes al caso (poliza, cobertura, normativa, historial),
 * cada uno con su fuente citable. La implementacion esconde el indice vectorial
 * detras de esta interfaz (cambiar de base vectorial no toca al agente).
 */
public interface ConocimientoPort {

    List<Evidencia> recuperar(SolicitudCopiloto solicitud);
}
