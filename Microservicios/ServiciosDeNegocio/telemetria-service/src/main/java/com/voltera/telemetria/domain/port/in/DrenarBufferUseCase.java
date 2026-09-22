package com.voltera.telemetria.domain.port.in;

/**
 * Puerto de ENTRADA: fuerza el desencolado (drenado) del buffer offline.
 * Lo invoca un job programado al recuperar conexion, y tambien puede
 * dispararse manualmente (para el experimento de store-and-forward).
 */
public interface DrenarBufferUseCase {

    ResultadoDrenado drenar();

    record ResultadoDrenado(int reenviados, int pendientes) {}
}
