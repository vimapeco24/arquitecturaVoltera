package com.voltera.certificados.infrastructure.rest.dto;

import com.voltera.certificados.domain.model.CertificadoRenovable;

import java.util.List;

/** Vista REST del certificado renovable con su cadena de custodia. */
public record CertificadoResponse(
        String id,
        String idMedidor,
        String emisor,
        String titular,
        long consumoNetoMensualWh,
        int periodo,
        String solicitudCertificacionId,
        String estadoDominio,
        String estadoCadena,
        Long tokenId,
        String txHash,
        String ultimoError,
        String creadoEn,
        List<MovimientoDto> historial) {

    public record MovimientoDto(String tipo, String desde, String hacia, String ts, String txHash) {}

    public static CertificadoResponse desde(CertificadoRenovable c) {
        List<MovimientoDto> hist = c.historial().stream()
                .map(m -> new MovimientoDto(
                        m.tipo(), m.desde(), m.hacia(),
                        m.ts() == null ? null : m.ts().toString(), m.txHash()))
                .toList();
        return new CertificadoResponse(
                c.id(), c.idMedidor(), c.emisor(), c.titular(), c.consumoNetoMensualWh(),
                c.periodo(), c.solicitudCertificacionId(), c.estadoDominio().name(),
                c.estadoCadena().name(), c.tokenId(), c.txHash(), c.ultimoError(),
                c.creadoEn() == null ? null : c.creadoEn().toString(), hist);
    }
}
