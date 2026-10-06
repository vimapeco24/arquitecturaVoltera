package com.voltera.telemetriacore.domain.port.out;

import com.voltera.telemetriacore.domain.model.ConsumoVista;

import java.util.List;

/** Puerto de SALIDA (lado QUERY/CQRS): proyeccion de consumo por intervalo. */
public interface ConsumoVistaRepositoryPort {
    ConsumoVista guardar(ConsumoVista vista);
    List<ConsumoVista> porMedidor(String medidorSerial);
    List<ConsumoVista> todas();
}
