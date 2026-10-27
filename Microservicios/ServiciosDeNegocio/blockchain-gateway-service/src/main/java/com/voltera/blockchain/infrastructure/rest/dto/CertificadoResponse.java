package com.voltera.blockchain.infrastructure.rest.dto;

import com.voltera.blockchain.domain.model.CertificadoNft;

import java.util.List;

/** Vista REST de un certificado NFT, con su cadena de custodia billetera->billetera. */
public record CertificadoResponse(
        long tokenId,
        String idMedidor,
        String propietario,
        String emisor,
        long consumoNetoMensualWh,
        int periodo,
        String tokenUri,
        String hashDetalle,
        String emitidoEn,
        boolean quemado,
        List<MovimientoDto> historial) {

    public record MovimientoDto(
            String tipo, String desde, String hacia, String ejecutadoPor, String ts, String txHash, String nota) {}

    public static CertificadoResponse de(CertificadoNft c) {
        List<MovimientoDto> hist = c.historial().stream()
                .map(m -> new MovimientoDto(
                        m.tipo().name(), m.desde(), m.hacia(), m.ejecutadoPor(),
                        m.ts() == null ? null : m.ts().toString(), m.txHash(), m.nota()))
                .toList();
        return new CertificadoResponse(
                c.tokenId(), c.idMedidor(), c.propietario(), c.emisor(),
                c.consumoNetoMensualWh(), c.periodo(), c.tokenUri(), c.hashDetalle(),
                c.emitidoEn() == null ? null : c.emitidoEn().toString(), c.quemado(), hist);
    }
}
