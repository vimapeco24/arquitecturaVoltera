package com.voltera.blockchain.infrastructure.scheduler;

import com.voltera.blockchain.application.AnclajeService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Relay del patron OUTBOX: de forma periodica empuja a la cadena los anclajes que
 * quedaron PENDIENTE (porque el RPC del tercero no estaba disponible o fallo un
 * intento). Mismo patron que el relay programado del resto de servicios EDA.
 */
@Component
public class OutboxRelay {

    private final AnclajeService anclajeService;

    public OutboxRelay(AnclajeService anclajeService) {
        this.anclajeService = anclajeService;
    }

    @Scheduled(fixedDelayString = "${voltera.blockchain.relay-delay-ms:15000}")
    public void procesar() {
        anclajeService.procesarPendientes();
    }
}
