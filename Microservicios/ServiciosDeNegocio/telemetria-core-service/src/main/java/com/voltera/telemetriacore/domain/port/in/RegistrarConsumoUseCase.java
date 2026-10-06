package com.voltera.telemetriacore.domain.port.in;

import com.voltera.telemetriacore.domain.event.LecturaValidada;
import com.voltera.telemetriacore.domain.model.ConsumoVista;

import java.util.List;

/**
 * Puerto de ENTRADA: MS Telemetria Core (CQRS, lamina 02).
 */
public interface RegistrarConsumoUseCase {

    /** Incorpora una LecturaValidada a la serie del medidor (lado command). */
    void registrarLectura(LecturaValidada lectura);

    /** Reaccion a MedidorHabilitado: crea la serie del medidor si no existe. */
    void crearSerie(String medidorSerial);

    /** Barre series y emite MedidorSinReporte para las que superaron la ventana. */
    int revisarSinReporte();

    /** Consulta (lado QUERY): consumo por intervalo de un medidor. */
    List<ConsumoVista> consumoPorMedidor(String medidorSerial);

    /** Consulta (lado QUERY): todas las vistas. */
    List<ConsumoVista> todas();
}
