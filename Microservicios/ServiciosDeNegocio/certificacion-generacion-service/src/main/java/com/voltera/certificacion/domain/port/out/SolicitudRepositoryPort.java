package com.voltera.certificacion.domain.port.out;

import com.voltera.certificacion.domain.model.BalanceGeneracionCertificable;
import com.voltera.certificacion.domain.model.SolicitudCertificacion;

import java.util.List;
import java.util.Optional;

/**
 * Puerto de SALIDA: persistencia del balance por medidor y de las solicitudes.
 * Es almacenamiento OFF-CHAIN (estado local del microservicio).
 */
public interface SolicitudRepositoryPort {

    BalanceGeneracionCertificable guardarBalance(BalanceGeneracionCertificable balance);

    Optional<BalanceGeneracionCertificable> buscarBalance(String medidorId);

    List<BalanceGeneracionCertificable> todosLosBalances();

    SolicitudCertificacion guardarSolicitud(SolicitudCertificacion solicitud);

    Optional<SolicitudCertificacion> buscarSolicitud(String id);

    List<SolicitudCertificacion> todasLasSolicitudes();
}
