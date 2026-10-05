package com.voltera.tarifaeventos.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.voltera.tarifaeventos.application.ProcessManagerAltaMedidor;
import com.voltera.tarifaeventos.application.TarifaEventosService;
import com.voltera.tarifaeventos.domain.port.out.CargoRepositoryPort;
import com.voltera.tarifaeventos.domain.port.out.CargoVistaRepositoryPort;
import com.voltera.tarifaeventos.domain.port.out.InboxPort;
import com.voltera.tarifaeventos.domain.port.out.NotificacionPublisherPort;
import com.voltera.tarifaeventos.domain.port.out.OutboxPort;
import com.voltera.tarifaeventos.domain.port.out.SagaRepositoryPort;
import com.voltera.tarifaeventos.domain.port.out.SerializadorEventosPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Wiring de la capa de aplicacion (arquitectura hexagonal).
 */
@Configuration
@EnableScheduling
public class BeanConfiguration {

    @Bean
    public TarifaEventosService tarifaEventosService(CargoRepositoryPort escrituraRepo,
                                                     CargoVistaRepositoryPort lecturaRepo,
                                                     NotificacionPublisherPort notificacionPublisher,
                                                     InboxPort inbox) {
        return new TarifaEventosService(escrituraRepo, lecturaRepo, notificacionPublisher, inbox);
    }

    @Bean
    public ProcessManagerAltaMedidor processManagerAltaMedidor(SagaRepositoryPort sagas,
                                                               OutboxPort outbox,
                                                               SerializadorEventosPort serializador) {
        return new ProcessManagerAltaMedidor(sagas, outbox, serializador);
    }

    /** ObjectMapper con soporte para java.time (Instant) usado por consumer y publisher. */
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper().registerModule(new JavaTimeModule());
    }
}
