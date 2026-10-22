package com.voltera.liquidacion.infrastructure.config;

import com.voltera.liquidacion.application.LiquidacionService;
import com.voltera.liquidacion.domain.port.out.AnclajeBlockchainPort;
import com.voltera.liquidacion.domain.port.out.LiquidacionEventPublisherPort;
import com.voltera.liquidacion.domain.port.out.TransaccionRepositoryPort;
import com.voltera.liquidacion.infrastructure.blockchain.NoopAnclajeBlockchainAdapter;
import com.voltera.liquidacion.infrastructure.blockchain.RestAnclajeBlockchainAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wiring hexagonal: conecta el servicio de aplicacion con los adaptadores de
 * salida (persistencia y, opcionalmente, el gateway de blockchain). El
 * dominio/aplicacion no dependen de Spring; solo esta capa.
 */
@Configuration
public class BeanConfiguration {

    private static final Logger log = LoggerFactory.getLogger(BeanConfiguration.class);

    /**
     * Adaptador hacia el blockchain-gateway. Si 'voltera.blockchain.gateway-url'
     * esta vacio (por defecto), se usa el NO-OP: el servicio se comporta como
     * siempre. Si se define (p. ej. la URL publica de la PaaS o la ruta WSO2),
     * cada liquidacion solicita su anclaje por REST (best-effort, asincrono).
     */
    @Bean
    public AnclajeBlockchainPort anclajeBlockchainPort(
            @Value("${voltera.blockchain.gateway-url:}") String gatewayUrl,
            @Value("${voltera.blockchain.anclar-path:/api/v1/anclajes}") String anclarPath) {
        if (gatewayUrl == null || gatewayUrl.isBlank()) {
            log.info("blockchain-gateway NO configurado (voltera.blockchain.gateway-url vacio): "
                    + "anclaje deshabilitado (no-op).");
            return new NoopAnclajeBlockchainAdapter();
        }
        log.info("blockchain-gateway configurado en {} (path {}): las liquidaciones se anclaran.",
                gatewayUrl, anclarPath);
        return new RestAnclajeBlockchainAdapter(gatewayUrl, anclarPath);
    }

    @Bean
    public LiquidacionService liquidacionService(TransaccionRepositoryPort repositorio,
                                                 AnclajeBlockchainPort anclajeBlockchainPort,
                                                 LiquidacionEventPublisherPort eventPublisher) {
        return new LiquidacionService(repositorio, anclajeBlockchainPort, eventPublisher);
    }
}
