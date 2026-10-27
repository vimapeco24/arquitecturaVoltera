package com.voltera.certificacion.domain.port.in;

import com.voltera.certificacion.domain.model.BalanceGeneracionCertificable;
import com.voltera.certificacion.domain.model.SolicitudCertificacion;

import java.util.List;

/**
 * Puerto de ENTRADA del Servicio de Certificación de Generación (lámina 04).
 * NO toca la cadena: es dominio puro off-chain.
 */
public interface CertificarGeneracionUseCase {

    /** Registra una medición de generación de Telemetría (idempotente, no reutiliza). */
    record ComandoRegistrarGeneracion(String medidorId, String medicionId, long wh) {}

    /** Crea una solicitud de certificación de un bloque reservado. */
    record ComandoSolicitar(String medidorId, String propietario, int periodo) {}

    BalanceGeneracionCertificable registrarGeneracion(ComandoRegistrarGeneracion cmd);

    /**
     * Reserva un bloque (1 MWh) del balance del medidor y abre una
     * SolicitudCertificacion PENDIENTE. Falla si no hay generación suficiente.
     */
    SolicitudCertificacion solicitarCertificacion(ComandoSolicitar cmd);

    SolicitudCertificacion avalarVoltera(String solicitudId);

    SolicitudCertificacion avalarCertificadora(String solicitudId);

    SolicitudCertificacion rechazar(String solicitudId, String motivo);

    SolicitudCertificacion porId(String solicitudId);

    List<SolicitudCertificacion> todas();

    BalanceGeneracionCertificable balanceDe(String medidorId);

    List<BalanceGeneracionCertificable> balances();
}
