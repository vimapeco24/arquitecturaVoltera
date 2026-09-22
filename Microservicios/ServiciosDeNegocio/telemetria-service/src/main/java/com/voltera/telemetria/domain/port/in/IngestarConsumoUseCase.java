package com.voltera.telemetria.domain.port.in;

import com.voltera.telemetria.domain.model.ConsumoRegistrado;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Puerto de ENTRADA: ingesta de una lectura de consumo desde el medidor IoT.
 *
 * <p>El resultado indica si el evento se publico directamente al broker o si
 * quedo en el buffer offline (store-and-forward) por falta de conexion.</p>
 */
public interface IngestarConsumoUseCase {

    ResultadoIngesta ingestar(ComandoIngestarConsumo comando);

    record ComandoIngestarConsumo(
            String medidorId,
            BigDecimal consumoKwh,
            Instant capturadaEn
    ) {}

    record ResultadoIngesta(
            ConsumoRegistrado evento,
            boolean publicadoEnBroker,
            boolean enBufferOffline
    ) {}
}
