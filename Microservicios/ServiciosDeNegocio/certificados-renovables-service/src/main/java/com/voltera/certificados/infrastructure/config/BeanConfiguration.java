package com.voltera.certificados.infrastructure.config;

import com.voltera.certificados.application.CertificadosService;
import com.voltera.certificados.domain.port.out.CertificadoRepositoryPort;
import com.voltera.certificados.domain.port.out.GatewayCadenaPort;
import com.voltera.certificados.infrastructure.gateway.NoopGatewayCadenaAdapter;
import com.voltera.certificados.infrastructure.gateway.RestGatewayCadenaAdapter;
import com.voltera.certificados.infrastructure.persistence.InMemoryCertificadoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wiring hexagonal. Elige el adaptador de gateway según haya URL configurada:
 * patrón puerto-opcional (REST si hay URL; demo/no-op si está vacía).
 */
@Configuration
public class BeanConfiguration {

    private static final Logger log = LoggerFactory.getLogger(BeanConfiguration.class);

    @Bean
    public CertificadoRepositoryPort certificadoRepository() {
        return new InMemoryCertificadoRepository();
    }

    @Bean
    public GatewayCadenaPort gatewayCadenaPort(
            @Value("${voltera.blockchain.gateway-url:}") String gatewayUrl,
            @Value("${voltera.blockchain.certificados-path:/api/v1/certificados}") String path) {
        if (gatewayUrl == null || gatewayUrl.isBlank()) {
            log.info("blockchain-gateway NO configurado (voltera.blockchain.gateway-url vacío): "
                    + "certificados en modo DEMO (el gateway se simula; estado local).");
            return new NoopGatewayCadenaAdapter();
        }
        log.info("blockchain-gateway configurado en {} (path {}): las operaciones de NFT se "
                + "solicitarán al gateway (ACL).", gatewayUrl, path);
        return new RestGatewayCadenaAdapter(gatewayUrl, path);
    }

    @Bean
    public CertificadosService certificadosService(CertificadoRepositoryPort repo, GatewayCadenaPort gateway) {
        return new CertificadosService(repo, gateway);
    }
}
