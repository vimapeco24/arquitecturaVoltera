package com.voltera.ingesta.domain.port.out;

import com.voltera.ingesta.domain.model.SesionDeIngesta;

/**
 * Puerto de SALIDA: mantiene la sesion de ingesta (una por nodo en este laboratorio).
 */
public interface SesionRepositoryPort {
    SesionDeIngesta obtener();
}
