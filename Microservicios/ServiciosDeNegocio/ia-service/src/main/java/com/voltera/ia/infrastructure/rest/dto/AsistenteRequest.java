package com.voltera.ia.infrastructure.rest.dto;

import com.voltera.ia.domain.model.asistente.Audiencia;
import com.voltera.ia.domain.model.asistente.PreguntaAsistente;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Pregunta al Assistant sobre un diagnóstico concreto, antes de firmar. La
 * audiencia selecciona el prompt (prosumidor: simple; operador: técnico).
 */
public record AsistenteRequest(
        @NotBlank String diagnosticoId,
        @NotNull Audiencia audiencia,
        String usuario,
        @NotBlank String texto
) {
    public PreguntaAsistente aDominio() {
        return new PreguntaAsistente(UUID.fromString(diagnosticoId), audiencia,
                usuario == null ? "firmante-anonimo" : usuario, texto);
    }
}
