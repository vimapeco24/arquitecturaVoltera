package com.voltera.ia.domain.port.in;

import com.voltera.ia.domain.model.asistente.PreguntaAsistente;
import com.voltera.ia.domain.model.asistente.RespuestaAsistente;

/**
 * Assistant conversacional acotado al diagnóstico, previo a la firma. Responde
 * con el diagnóstico, la propuesta y la evidencia RAG de ese caso; NO tiene
 * herramientas de acción (no firma, no crea propuestas, no ejecuta comandos) y
 * un guardrail le impide persuadir al firmante.
 */
public interface AsistenteUseCase {

    RespuestaAsistente responder(PreguntaAsistente pregunta);
}
