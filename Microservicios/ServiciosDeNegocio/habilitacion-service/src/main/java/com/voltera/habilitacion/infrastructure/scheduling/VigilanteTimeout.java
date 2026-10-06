package com.voltera.habilitacion.infrastructure.scheduling;

import com.voltera.habilitacion.domain.port.in.HabilitarMedidorUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Vigilante del plazo de alta (15 min). Suspende las habilitaciones no confirmadas
 * emitiendo HabilitacionFallida + MedidorSuspendido.
 */
@Component
public class VigilanteTimeout {

    private static final Logger log = LoggerFactory.getLogger(VigilanteTimeout.class);

    private final HabilitarMedidorUseCase useCase;

    public VigilanteTimeout(HabilitarMedidorUseCase useCase) {
        this.useCase = useCase;
    }

    @Scheduled(fixedDelayString = "${habilitacion.timeout.check-interval-ms:5000}")
    public void revisar() {
        int n = useCase.procesarTimeouts();
        if (n > 0) {
            log.info("Habilitacion: {} medidor(es) suspendido(s) por timeout de 15 min", n);
        }
    }
}
