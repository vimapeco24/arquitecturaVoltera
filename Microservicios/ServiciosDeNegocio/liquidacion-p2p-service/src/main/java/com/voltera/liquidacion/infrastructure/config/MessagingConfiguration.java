package com.voltera.liquidacion.infrastructure.config;

import com.voltera.liquidacion.domain.port.out.LiquidacionEventPublisherPort;
import com.voltera.liquidacion.infrastructure.messaging.KafkaLiquidacionEventPublisher;
import com.voltera.liquidacion.infrastructure.messaging.NoopLiquidacionEventPublisher;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Wiring del adaptador de salida EDA (publicación de LiquidacionCalculada).
 *
 * <p>Por defecto el publisher es NO-OP: el servicio funciona igual que siempre
 * (anclaje solo por REST). Si {@code voltera.kafka.enabled=true}, se crea el
 * productor Kafka y el publisher que emite a 'topic.facturacion.liquidacion',
 * cerrando la orquestación por eventos con el blockchain-gateway (que ya
 * consume ese topic).</p>
 */
@Configuration
public class MessagingConfiguration {

    private static final Logger log = LoggerFactory.getLogger(MessagingConfiguration.class);

    @Value("${voltera.eventos.topic-liquidacion:topic.facturacion.liquidacion}")
    private String topicLiquidacion;

    // ---------- Modo EDA activo (voltera.kafka.enabled=true) ----------

    @Bean
    @ConditionalOnProperty(name = "voltera.kafka.enabled", havingValue = "true")
    public ProducerFactory<String, String> liquidacionProducerFactory(
            @Value("${spring.kafka.bootstrap-servers:redpanda.voltera-app:9092}") String bootstrapServers) {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        // Best-effort: no bloquear peticiones si el broker no responde.
        config.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, 2000);
        config.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, 5000);
        config.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 3000);
        return new DefaultKafkaProducerFactory<>(config);
    }

    @Bean
    @ConditionalOnProperty(name = "voltera.kafka.enabled", havingValue = "true")
    public KafkaTemplate<String, String> liquidacionKafkaTemplate(ProducerFactory<String, String> liquidacionProducerFactory) {
        return new KafkaTemplate<>(liquidacionProducerFactory);
    }

    @Bean
    @ConditionalOnProperty(name = "voltera.kafka.enabled", havingValue = "true")
    public LiquidacionEventPublisherPort kafkaLiquidacionEventPublisher(KafkaTemplate<String, String> liquidacionKafkaTemplate) {
        log.info("EDA habilitado: LiquidacionCalculada se publicará en el topic '{}'.", topicLiquidacion);
        return new KafkaLiquidacionEventPublisher(liquidacionKafkaTemplate, topicLiquidacion);
    }

    // ---------- Modo por defecto (sin Kafka): no-op ----------

    @Bean
    @ConditionalOnProperty(name = "voltera.kafka.enabled", havingValue = "false", matchIfMissing = true)
    public LiquidacionEventPublisherPort noopLiquidacionEventPublisher() {
        log.info("EDA deshabilitado (voltera.kafka.enabled=false): LiquidacionCalculada NO se publica (anclaje solo por REST).");
        return new NoopLiquidacionEventPublisher();
    }
}
