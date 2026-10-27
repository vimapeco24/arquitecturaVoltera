package com.voltera.certificacion.infrastructure.rest.dto;

import com.voltera.certificacion.domain.model.SolicitudCertificacion;

/** Vista REST de una solicitud de certificación. */
public record SolicitudResponse(
        String id,
        String medidorId,
        String propietario,
        long energiaWh,
        int periodo,
        String estado,
        boolean avalVoltera,
        boolean avalCertificadora,
        String motivoRechazo,
        String creadaEn,
        String resueltaEn) {

    public static SolicitudResponse desde(SolicitudCertificacion s) {
        return new SolicitudResponse(
                s.id(), s.medidorId(), s.propietario(), s.energiaWh(), s.periodo(),
                s.estado().name(), s.avalVoltera(), s.avalCertificadora(), s.motivoRechazo(),
                s.creadaEn() == null ? null : s.creadaEn().toString(),
                s.resueltaEn() == null ? null : s.resueltaEn().toString());
    }
}
