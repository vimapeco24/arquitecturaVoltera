package com.voltera.integracionami.domain.model;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Agregado raiz del BC <b>Integracion AMI</b> (Adaptador de proveedores / ACL,
 * lamina 02).
 *
 * <p>Representa la conexion con el head-end de un proveedor AMI mediante un
 * {@link Protocolo}. Es un <b>Anti-Corruption Layer</b>: recibe lotes de
 * {@link LecturaCruda} en el formato del proveedor y los traduce al formato
 * canonico de Voltera. Solo atiende medidores que fueron habilitados
 * ({@code MedidorHabilitado}) y aun no suspendidos ({@code MedidorSuspendido}).</p>
 */
public class ConexionHeadEnd {

    private final String proveedorAmi;
    private final Protocolo protocolo;
    private EstadoProveedor estado;

    /** Seriales de medidores habilitados que esta conexion debe atender. */
    private final Set<String> medidoresHabilitados = ConcurrentHashMap.newKeySet();

    public ConexionHeadEnd(String proveedorAmi, Protocolo protocolo) {
        if (proveedorAmi == null || proveedorAmi.isBlank()) {
            throw new IllegalArgumentException("proveedorAmi es obligatorio");
        }
        this.proveedorAmi = proveedorAmi;
        this.protocolo = protocolo != null ? protocolo : Protocolo.MQTT;
        this.estado = EstadoProveedor.OPERATIVO;
    }

    /** Registra un medidor habilitado (reaccion a MedidorHabilitado). */
    public void habilitarMedidor(String medidorSerial) {
        if (medidorSerial != null && !medidorSerial.isBlank()) {
            medidoresHabilitados.add(medidorSerial);
        }
    }

    /** Retira un medidor suspendido (reaccion a MedidorSuspendido). */
    public void suspenderMedidor(String medidorSerial) {
        medidoresHabilitados.remove(medidorSerial);
    }

    public boolean atiende(String medidorSerial) {
        return medidoresHabilitados.contains(medidorSerial);
    }

    public void marcarDegradado() { this.estado = EstadoProveedor.DEGRADADO; }
    public void marcarOperativo() { this.estado = EstadoProveedor.OPERATIVO; }

    public String proveedorAmi() { return proveedorAmi; }
    public Protocolo protocolo() { return protocolo; }
    public EstadoProveedor estado() { return estado; }
    public int totalMedidores() { return medidoresHabilitados.size(); }
}
