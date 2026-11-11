package com.voltera.ia.infrastructure.rest.dto;

import com.voltera.ia.domain.model.Evidencia;
import com.voltera.ia.domain.model.PasoFlujo;
import com.voltera.ia.domain.model.RecomendacionCopiloto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Respuesta del copiloto hacia el frontend: la propuesta, su justificacion, la
 * evidencia citada y la traza del flujo agentico (para la consola del analista).
 */
public record RecomendacionResponse(
        String recomendacionId,
        String siniestroId,
        String decisionPropuesta,
        BigDecimal montoSugerido,
        String confianza,
        boolean requiereRevisionHumana,
        String justificacion,
        String motor,
        boolean datosEnmascarados,
        List<EvidenciaDto> evidencias,
        List<PasoDto> traza,
        Instant generadaEn
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

    public static RecomendacionResponse desde(RecomendacionCopiloto r) {
        return new RecomendacionResponse(
                r.recomendacionId().toString(),
                r.siniestroId(),
                r.decisionPropuesta().name(),
                r.montoSugerido(),
                r.confianza().name(),
                r.requiereRevisionHumana(),
                r.justificacion(),
                r.motor(),
                r.datosEnmascarados(),
                r.evidencias().stream().map(EvidenciaDto::desde).toList(),
                r.traza().stream().map(PasoDto::desde).toList(),
                r.generadaEn());
    }
}
