package com.voltera.ia.domain.model.der;

import com.voltera.ia.domain.model.Evidencia;
import com.voltera.ia.domain.model.PasoFlujo;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Resultado del flujo agéntico de diagnóstico de activos DER. Es una PROPUESTA
 * de los tres agentes (Monitor → Diagnosticador → Acción), no una decisión: la
 * ejecución de comandos físicos o el despacho de técnicos la aprueba una persona
 * (Human Review). Incluye la evidencia citada (RAG + herramientas MCP), la traza
 * agéntica y los metadatos de gobierno (prompt/modelo, identidad, auditoría).
 */
public record DiagnosticoDer(
        UUID diagnosticoId,
        String activoId,
        TipoActivo tipoActivo,
        String comunidadId,
        Severidad severidad,
        String diagnostico,
        TipoAccion accionPropuesta,
        String ordenTrabajo,
        boolean requiereAprobacionHumana,
        List<Evidencia> evidencias,
        List<PasoFlujo> traza,
        String motor,
        boolean datosEnmascarados,
        String promptVersion,
        String usuario,
        Instant generadoEn
) {
    public static DiagnosticoDer crear(
            String activoId, TipoActivo tipoActivo, String comunidadId, Severidad severidad,
            String diagnostico, TipoAccion accionPropuesta, String ordenTrabajo,
            boolean requiereAprobacionHumana, List<Evidencia> evidencias, List<PasoFlujo> traza,
            String motor, boolean datosEnmascarados, String promptVersion, String usuario) {
        return new DiagnosticoDer(
                UUID.randomUUID(), activoId, tipoActivo, comunidadId, severidad, diagnostico,
                accionPropuesta, ordenTrabajo, requiereAprobacionHumana,
                List.copyOf(evidencias), List.copyOf(traza), motor, datosEnmascarados,
                promptVersion, usuario, Instant.now());
    }
}
