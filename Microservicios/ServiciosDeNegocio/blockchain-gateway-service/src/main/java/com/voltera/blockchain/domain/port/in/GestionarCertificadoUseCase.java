package com.voltera.blockchain.domain.port.in;

import com.voltera.blockchain.domain.model.CertificadoNft;

import java.util.List;

/**
 * Puerto de ENTRADA para el ciclo de vida de los certificados de origen (NFT
 * ERC-721): emitir, transferir de billetera a billetera, quemar y consultar.
 */
public interface GestionarCertificadoUseCase {

    /** Datos para acuñar un certificado. */
    record ComandoEmitir(
            String emisor,
            String propietario,
            String idMedidor,
            long consumoNetoMensualWh,
            int periodo) {}

    /**
     * Acuña un certificado NUEVO. Rechaza duplicados para el mismo (medidor, periodo).
     * @throws IllegalArgumentException si los datos son invalidos o ya existe.
     */
    CertificadoNft emitir(ComandoEmitir cmd);

    /**
     * Transfiere un certificado de su propietario actual a {@code hacia}.
     * @throws IllegalArgumentException si el token no existe, esta quemado o el destino es invalido.
     */
    CertificadoNft transferir(long tokenId, String hacia);

    /** Quema (retira) un certificado. */
    CertificadoNft quemar(long tokenId);

    CertificadoNft porToken(long tokenId);

    List<CertificadoNft> todos();

    /** Certificados cuyo propietario ACTUAL es la billetera dada. */
    List<CertificadoNft> porPropietario(String propietario);
}
