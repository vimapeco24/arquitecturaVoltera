package com.voltera.pagos.infrastructure.config;

import com.voltera.pagos.application.OutboxEventosPago;
import com.voltera.pagos.application.PagoService;
import com.voltera.pagos.domain.port.out.OutboxPort;
import com.voltera.pagos.domain.port.out.PagoRepositoryPort;
import com.voltera.pagos.domain.port.out.SerializadorEventosPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
public class BeanConfiguration {

    /**
     * Con {@code pagos.kafka.enabled=false} no se registra outbox ni se publican eventos.
     */
    @Bean
    public PagoService pagoService(PagoRepositoryPort repositorio,
                                   OutboxPort outbox,
                                   SerializadorEventosPort serializador,
                                   @Value("${pagos.kafka.enabled:true}") boolean kafkaHabilitado) {
        if (!kafkaHabilitado) {
            return new PagoService(repositorio);
        }
        return new PagoService(repositorio, new OutboxEventosPago(outbox, serializador));
    }
}
