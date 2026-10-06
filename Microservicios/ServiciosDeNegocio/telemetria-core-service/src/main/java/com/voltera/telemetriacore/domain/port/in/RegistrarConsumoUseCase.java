package com.voltera.telemetriacore.domain.port.in;

import com.voltera.telemetriacore.domain.event.LecturaValidada;
import com.voltera.telemetriacore.domain.model.ConsumoAgregadoVista;
import com.voltera.telemetriacore.domain.model.ConsumoVista;

import java.util.List;
import java.util.Optional;

/**
 * Puerto de ENTRADA: MS Telemetria Core (CQRS, lamina 02/03).
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

    /** Consulta (lado QUERY): todas las vistas por intervalo. */
    List<ConsumoVista> todas();

    /** Consulta (lado QUERY, vista agregada): acumulado de un medidor. */
    Optional<ConsumoAgregadoVista> agregadoPorMedidor(String medidorSerial);

    /** Consulta (lado QUERY, vista agregada): acumulado de todos los medidores. */
    List<ConsumoAgregadoVista> agregados();
}
