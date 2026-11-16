package com.voltera.ia.domain.port.out;

import com.voltera.ia.domain.model.der.DiagnosticoDer;

import java.util.Optional;
import java.util.UUID;

/**
 * State &amp; Memory de los diagnósticos generados. El Assistant y la gobernanza
 * recuperan por id el diagnóstico concreto para acotar su contexto (el Assistant
 * responde SOLO sobre ese caso; la gobernanza crea la propuesta a partir de él).
 */
public interface DiagnosticoRepositoryPort {

    DiagnosticoDer guardar(DiagnosticoDer diagnostico);

    Optional<DiagnosticoDer> porId(UUID diagnosticoId);
}
