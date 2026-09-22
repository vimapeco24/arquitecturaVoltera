package com.voltera.telemetria.infrastructure.rest.dto;

import com.voltera.telemetria.domain.port.in.IngestarConsumoUseCase.ResultadoIngesta;

import java.math.BigDecimal;

/**
 * Respuesta de la ingesta: refleja si el evento se publico al broker o quedo en
 * el buffer offline (store-and-forward), ademas de los datos del evento.
 */
public record IngestaResponse(
        String eventId,
        String medidorId,
        String prosumidorId,
        BigDecimal consumoKwh,
        BigDecimal umbralKwh,
        boolean consumoExtra,
        boolean publicadoEnBroker,
        boolean enBufferOffline,
        String ocurridoEn
) {
    public static IngestaResponse desde(ResultadoIngesta r) {
        var e = r.evento();
        return new IngestaResponse(
                e.eventId().toString(),
                e.medidorId(),
                e.prosumidorId(),
                e.consumoKwh(),
                e.umbralKwh(),
                e.consumoExtra(),
                r.publicadoEnBroker(),
                r.enBufferOffline(),
                e.ocurridoEn().toString()
        );
    }
}
