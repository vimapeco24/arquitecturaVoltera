package com.voltera.certificados;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Servicio de Certificados Renovables (Módulo 4 · lámina 04).
 *
 * <p>BC: Certificados renovables. Agregado {@code CertificadoRenovable}. Gestiona
 * la emisión, titularidad, transferencia y retiro del certificado. <b>No toca la
 * cadena directamente</b>: solicita las operaciones al <b>gateway</b> (que sí
 * habla web3j) y actualiza su estado tras recibir la confirmación. La traducción
 * dominio → gateway se hace en un <b>anticorruption layer</b>.</p>
 */
@SpringBootApplication
public class CertificadosRenovablesApplication {
    public static void main(String[] args) {
        SpringApplication.run(CertificadosRenovablesApplication.class, args);
    }
}
