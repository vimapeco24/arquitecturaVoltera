package com.voltera.certificados.domain.port.out;

import com.voltera.certificados.domain.model.CertificadoRenovable;

import java.util.List;
import java.util.Optional;

/** Puerto de SALIDA: persistencia OFF-CHAIN del agregado CertificadoRenovable. */
public interface CertificadoRepositoryPort {
    CertificadoRenovable guardar(CertificadoRenovable certificado);
    Optional<CertificadoRenovable> buscar(String id);
    List<CertificadoRenovable> todos();
}
