package com.voltera.certificacion;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Servicio de Certificación de Generación (Módulo 4 · lámina 04).
 *
 * <p>BC: Certificación de generación. Agregados {@code BalanceGeneracionCertificable}
 * y {@code SolicitudCertificacion}. Consume la generación registrada por Telemetría,
 * reserva bloques de 1 MWh sin reutilizar mediciones y reúne los avales de Voltera y
 * la certificadora externa. <b>NO toca la cadena</b> (es dominio puro off-chain).</p>
 */
@SpringBootApplication
public class CertificacionGeneracionApplication {
    public static void main(String[] args) {
        SpringApplication.run(CertificacionGeneracionApplication.class, args);
    }
}
