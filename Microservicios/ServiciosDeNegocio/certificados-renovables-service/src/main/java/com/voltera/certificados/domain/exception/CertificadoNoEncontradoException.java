package com.voltera.certificados.domain.exception;

public class CertificadoNoEncontradoException extends RuntimeException {
    public CertificadoNoEncontradoException(String id) {
        super("Certificado renovable no encontrado: " + id);
    }
}
