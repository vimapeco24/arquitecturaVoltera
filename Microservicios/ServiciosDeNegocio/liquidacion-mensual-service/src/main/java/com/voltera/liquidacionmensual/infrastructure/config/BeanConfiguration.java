package com.voltera.liquidacionmensual.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.voltera.liquidacionmensual.application.AcumuladorLiquidacionService;
import com.voltera.liquidacionmensual.application.LiquidacionMensualService;
import com.voltera.liquidacionmensual.domain.port.out.LiquidacionRepositoryPort;
import com.voltera.liquidacionmensual.domain.port.out.OutboxPort;
import com.voltera.liquidacionmensual.domain.port.out.SerializadorEventosPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
public class BeanConfiguration {

    @Bean
    public LiquidacionMensualService liquidacionMensualService(LiquidacionRepositoryPort repositorio) {
        return new LiquidacionMensualService(repositorio);
    }

    @Bean
    public AcumuladorLiquidacionService acumuladorLiquidacionService(
            OutboxPort outbox,
            SerializadorEventosPort serializador,
            @Value("${liquidacion.umbral-cierre-kwh:100.0}") double umbralCierreKwh) {
        return new AcumuladorLiquidacionService(outbox, serializador, umbralCierreKwh);
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper().registerModule(new JavaTimeModule());
    }
}
