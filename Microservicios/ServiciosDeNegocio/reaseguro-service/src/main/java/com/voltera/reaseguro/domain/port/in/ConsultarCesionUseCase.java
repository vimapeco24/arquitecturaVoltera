package com.voltera.reaseguro.domain.port.in;

import com.voltera.reaseguro.domain.model.CesionVista;

import java.util.List;

/**
 * Puerto de entrada del lado de LECTURA (consulta CQRS). Devuelve vistas
 * inmutables {@link CesionVista} desde el modelo de lectura.
 */
public interface ConsultarCesionUseCase {

    CesionVista porId(String id);

    List<CesionVista> porPoliza(String polizaId);

    List<CesionVista> todas();
}
