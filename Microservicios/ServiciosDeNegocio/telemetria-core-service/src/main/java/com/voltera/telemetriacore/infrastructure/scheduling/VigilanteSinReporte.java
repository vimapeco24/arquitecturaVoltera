package com.voltera.telemetriacore.infrastructure.scheduling;

import com.voltera.telemetriacore.domain.port.in.RegistrarConsumoUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Vigilante que emite MedidorSinReporte para medidores que dejaron de reportar
 * dentro de la ventana configurada.
 */
@Component
public class VigilanteSinReporte {

    private static final Logger log = LoggerFactory.getLogger(VigilanteSinReporte.class);

    private final RegistrarConsumoUseCase useCase;

    public VigilanteSinReporte(RegistrarConsumoUseCase useCase) {
        this.useCase = useCase;
    }

    @Scheduled(fixedDelayString = "${telemetria-core.sin-reporte.check-interval-ms:10000}")
    public void revisar() {
        int n = useCase.revisarSinReporte();
        if (n > 0) {
            log.info("Telemetria Core: {} MedidorSinReporte emitido(s)", n);
        }
    }
}
