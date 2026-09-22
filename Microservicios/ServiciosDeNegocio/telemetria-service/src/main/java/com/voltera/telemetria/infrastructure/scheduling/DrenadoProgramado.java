package com.voltera.telemetria.infrastructure.scheduling;

import com.voltera.telemetria.domain.port.in.DrenarBufferUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Job programado que implementa el "forward" del store-and-forward: cada pocos
 * segundos intenta desencolar (drenar) el buffer offline y reenviar los eventos
 * pendientes al broker. Si el broker sigue caido, no pasa nada; se reintenta en
 * el siguiente ciclo. Asi, al recuperar la conexion, los eventos acumulados
 * fluyen automaticamente sin intervencion manual.
 */
@Component
public class DrenadoProgramado {

    private static final Logger log = LoggerFactory.getLogger(DrenadoProgramado.class);

    private final DrenarBufferUseCase drenarBufferUseCase;

    public DrenadoProgramado(DrenarBufferUseCase drenarBufferUseCase) {
        this.drenarBufferUseCase = drenarBufferUseCase;
    }

    @Scheduled(fixedDelayString = "${telemetria.buffer.drain-interval-ms:5000}")
    public void drenar() {
        var resultado = drenarBufferUseCase.drenar();
        if (resultado.reenviados() > 0) {
            log.info("Buffer offline drenado: reenviados={}, pendientes={}",
                    resultado.reenviados(), resultado.pendientes());
        }
    }
}
