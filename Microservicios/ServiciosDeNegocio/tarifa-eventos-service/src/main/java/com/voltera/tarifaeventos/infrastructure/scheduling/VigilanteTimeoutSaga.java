package com.voltera.tarifaeventos.infrastructure.scheduling;

import com.voltera.tarifaeventos.domain.port.in.OrquestarAltaMedidorUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Vigilante de timeouts del process manager (lamina 10). Cada pocos segundos pide
 * compensar las sagas que superaron los 15 min sin confirmar
 * (CanalIngestaCreado + TarifaAsignada), emitiendo HabilitacionFallida +
 * MedidorSuspendido via outbox.
 */
@Component
public class VigilanteTimeoutSaga {

    private static final Logger log = LoggerFactory.getLogger(VigilanteTimeoutSaga.class);

    private final OrquestarAltaMedidorUseCase orquestador;

    public VigilanteTimeoutSaga(OrquestarAltaMedidorUseCase orquestador) {
        this.orquestador = orquestador;
    }

    @Scheduled(fixedDelayString = "${orquestacion.timeout.check-interval-ms:5000}")
    public void revisar() {
        int compensadas = orquestador.procesarTimeouts();
        if (compensadas > 0) {
            log.info("Process manager: {} saga(s) de alta vencidas por timeout de 15 min -> compensadas", compensadas);
        }
    }
}
