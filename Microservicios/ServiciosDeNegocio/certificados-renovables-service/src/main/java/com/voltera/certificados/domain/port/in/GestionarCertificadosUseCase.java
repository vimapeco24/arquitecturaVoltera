package com.voltera.certificados.domain.port.in;

import com.voltera.certificados.domain.model.CertificadoRenovable;

import java.util.List;

/**
 * Puerto de ENTRADA del Servicio de Certificados Renovables (lámina 04).
 * Gestiona emisión, titularidad, transferencia y retiro. NO habla web3j:
 * delega en el gateway a través del puerto de salida.
 */
public interface GestionarCertificadosUseCase {

    record ComandoEmitir(String idMedidor, String emisor, String titular,
                         long consumoNetoMensualWh, int periodo, String solicitudCertificacionId) {}

    CertificadoRenovable emitir(ComandoEmitir cmd);

    CertificadoRenovable transferir(String id, String hacia);

    CertificadoRenovable retirar(String id);

    CertificadoRenovable porId(String id);

    List<CertificadoRenovable> todos();

    List<CertificadoRenovable> porTitular(String titular);
}
