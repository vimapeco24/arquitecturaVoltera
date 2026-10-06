package com.voltera.telemetriacore.domain.model;

import java.time.Instant;

/**
 * Vista materializada del lado QUERY (CQRS): consumo por intervalo de un medidor.
 * La API de lectura sirve SOLO de aqui.
 */
public record ConsumoVista(
        String id,
        String medidorSerial,
        Instant inicioIntervalo,
        Instant finIntervalo,
        double consumoNetoKwh,
        long numLecturas,
        Instant registradoEn
) {
    public static ConsumoVista de(SerieDeMedicion.ConsumoNetoIntervalo c) {
        return new ConsumoVista(
                c.medidorSerial() + "@" + c.inicio().getEpochSecond(),
                c.medidorSerial(), c.inicio(), c.fin(),
                c.consumoNetoKwh(), c.numLecturas(), Instant.now());
    }
}
