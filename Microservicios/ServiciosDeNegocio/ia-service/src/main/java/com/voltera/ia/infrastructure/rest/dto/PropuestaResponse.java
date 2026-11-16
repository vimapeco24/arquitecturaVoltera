package com.voltera.ia.infrastructure.rest.dto;

import com.voltera.ia.domain.model.gobernanza.Firma;
import com.voltera.ia.domain.model.gobernanza.PropuestaAccion;

import java.time.Instant;
import java.util.List;

/**
 * Vista de una propuesta de acción gobernada 3/3 para la consola y la app: estado
 * del ciclo, firmas registradas (0/3 → 3/3), próximo firmante pendiente, y la
 * trazabilidad (hash del diagnóstico + versión de prompt/modelo) que va on-chain.
 */
public record PropuestaResponse(
        String propuestaId,
        String diagnosticoId,
        String activoId,
        String comunidadId,
        String tipoAccion,
        String comando,
        String severidad,
        String estado,
        String firmas,
        String siguienteFirmante,
        String hashDiagnostico,
        String versionPrompt,
        String versionModelo,
        String resultadoEjecucion,
        Instant creadaEn,
        Instant venceEn,
        List<FirmaDto> firmasDetalle
) {
    public record FirmaDto(String firmante, String identidad, boolean aprueba, String razon, Instant firmadoEn) {
        static FirmaDto desde(Firma f) {
            return new FirmaDto(f.firmante().name(), f.identidad(), f.aprueba(), f.razon(), f.firmadoEn());
        }
    }

    public static PropuestaResponse desde(PropuestaAccion p) {
        var siguiente = p.siguienteFirmantePendiente();
        return new PropuestaResponse(
                p.propuestaId().toString(), p.diagnosticoId().toString(), p.activoId(), p.comunidadId(),
                p.tipoAccion().name(), p.comando(), p.severidad().name(), p.estado().name(),
                p.firmasResumen(), siguiente == null ? null : siguiente.name(),
                p.hashDiagnostico(), p.versionPrompt(), p.versionModelo(), p.resultadoEjecucion(),
                p.creadaEn(), p.venceEn(),
                p.firmas().stream().map(FirmaDto::desde).toList());
    }
}
