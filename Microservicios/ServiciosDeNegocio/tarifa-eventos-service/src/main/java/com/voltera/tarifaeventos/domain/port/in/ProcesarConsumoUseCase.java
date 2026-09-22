package com.voltera.tarifaeventos.domain.port.in;

import com.voltera.tarifaeventos.domain.event.ConsumoRegistrado;
import com.voltera.tarifaeventos.domain.model.CargoTarifa;

/**
 * Puerto de ENTRADA: procesa el evento ConsumoRegistrado consumido del broker.
 * El procesamiento es idempotente por {@code eventId} y aplica CQRS
 * (escritura + proyeccion a la vista de lectura).
 */
public interface ProcesarConsumoUseCase {
    CargoTarifa procesar(ConsumoRegistrado evento);
}
