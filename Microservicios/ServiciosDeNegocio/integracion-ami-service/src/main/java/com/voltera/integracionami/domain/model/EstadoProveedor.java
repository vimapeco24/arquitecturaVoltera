package com.voltera.integracionami.domain.model;

/**
 * Salud de la conexion con el proveedor AMI. Si se degrada, se emite ProveedorDegradado.
 */
public enum EstadoProveedor {
    OPERATIVO,
    DEGRADADO
}
