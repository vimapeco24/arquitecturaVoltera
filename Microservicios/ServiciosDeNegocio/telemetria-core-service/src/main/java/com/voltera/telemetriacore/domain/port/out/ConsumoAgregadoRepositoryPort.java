package com.voltera.telemetriacore.domain.port.out;

import com.voltera.telemetriacore.domain.model.ConsumoAgregadoVista;

import java.util.List;
import java.util.Optional;

/**
 * Puerto de SALIDA (lado QUERY/CQRS): vista <b>agregada</b> de consumo por medidor.
 * El adaptador representa la "Vista de lectura · Redis + vistas agregadas" de la
 * lámina 03.
 */
public interface ConsumoAgregadoRepositoryPort {

    ConsumoAgregadoVista guardar(ConsumoAgregadoVista vista);

    Optional<ConsumoAgregadoVista> porMedidor(String medidorSerial);

    List<ConsumoAgregadoVista> todas();
}
