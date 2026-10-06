package com.voltera.integracionami.domain.port.in;

import com.voltera.integracionami.domain.model.ConexionHeadEnd;
import com.voltera.integracionami.domain.model.LecturaCruda;

import java.util.List;

/**
 * Puerto de ENTRADA: adaptador ACL del BC Integracion AMI (lamina 02).
 */
public interface RecibirLecturasUseCase {

    /**
     * Recibe un lote de lecturas crudas del head-end, las traduce a formato
     * canonico y emite LecturaCrudaRecibida por cada medidor ATENDIDO (habilitado).
     *
     * @return numero de lecturas canonicas emitidas.
     */
    int recibirLote(String proveedorAmi, List<LecturaCruda> lote);

    /** Reaccion a MedidorHabilitado: empezar a atender ese medidor. */
    void habilitarMedidor(String medidorSerial);

    /** Reaccion a MedidorSuspendido: dejar de atender ese medidor. */
    void suspenderMedidor(String medidorSerial);

    /** Marca el proveedor como degradado y emite ProveedorDegradado. */
    ConexionHeadEnd reportarDegradacion(String motivo);

    ConexionHeadEnd estado();
}
