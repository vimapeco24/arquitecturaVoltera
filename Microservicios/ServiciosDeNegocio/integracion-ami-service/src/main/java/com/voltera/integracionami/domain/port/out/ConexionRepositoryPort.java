package com.voltera.integracionami.domain.port.out;

import com.voltera.integracionami.domain.model.ConexionHeadEnd;

import java.util.Optional;

/**
 * Puerto de SALIDA: persistencia de la conexion con el head-end. En este caso de
 * laboratorio hay una unica conexion por proveedor principal.
 */
public interface ConexionRepositoryPort {
    ConexionHeadEnd obtener();
    ConexionHeadEnd guardar(ConexionHeadEnd conexion);
}
