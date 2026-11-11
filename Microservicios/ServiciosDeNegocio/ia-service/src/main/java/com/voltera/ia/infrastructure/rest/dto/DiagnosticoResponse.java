package com.voltera.ia.infrastructure.rest.dto;

import com.voltera.ia.domain.model.Evidencia;
import com.voltera.ia.domain.model.PasoFlujo;
import com.voltera.ia.domain.model.der.DiagnosticoDer;

import java.time.Instant;
import java.util.List;

/**
 * Respuesta del Agente de diagnóstico DER hacia el frontend: el diagnóstico, la
 * acción propuesta, si exige aprobación humana, la evidencia citada (RAG + MCP),
 * la traza de los tres agentes y los metadatos de gobierno (prompt, usuario).
 */
public record DiagnosticoResponse(
        String diagnosticoId,
        String activoId,
        String tipoActivo,
        String comunidadId,
        String severidad,
        String diagnostico,
        String accionPropuesta,
        String ordenTrabajo,
        boolean requiereAprobacionHumana,
        String motor,
        boolean datosEnmascarados,
        String promptVersion,
        String usuario,
        List<EvidenciaDto> evidencias,
        List<PasoDto> traza,
        Instant generadoEn
) {
    public record EvidenciaDto(String fuente, String titulo, String detalle) {
        static EvidenciaDto desde(Evidencia e) {
            return new EvidenciaDto(e.fuente(), e.titulo(), e.detalle());
        }
    }

    public record PasoDto(int orden, String nodo, String patron, String detalle, Instant ejecutadoEn) {
        static PasoDto desde(PasoFlujo p) {
            return new PasoDto(p.orden(), p.nodo(), p.patron(), p.detalle(), p.ejecutadoEn());
        }
    }

    public static DiagnosticoResponse desde(DiagnosticoDer d) {
        return new DiagnosticoResponse(
                d.diagnosticoId().toString(), d.activoId(), d.tipoActivo().name(), d.comunidadId(),
                d.severidad().name(), d.diagnostico(), d.accionPropuesta().name(), d.ordenTrabajo(),
                d.requiereAprobacionHumana(), d.motor(), d.datosEnmascarados(), d.promptVersion(),
                d.usuario(),
                d.evidencias().stream().map(EvidenciaDto::desde).toList(),
                d.traza().stream().map(PasoDto::desde).toList(),
                d.generadoEn());
    }
}
