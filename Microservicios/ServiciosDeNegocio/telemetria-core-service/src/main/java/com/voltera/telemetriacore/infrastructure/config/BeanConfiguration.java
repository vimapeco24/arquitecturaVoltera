package com.voltera.telemetriacore.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.voltera.telemetriacore.application.ProyectorVista;
import com.voltera.telemetriacore.application.TelemetriaCoreService;
import com.voltera.telemetriacore.domain.port.out.ConsumoAgregadoRepositoryPort;
import com.voltera.telemetriacore.domain.port.out.ConsumoVistaRepositoryPort;
import com.voltera.telemetriacore.domain.port.out.OutboxPort;
import com.voltera.telemetriacore.domain.port.out.SerializadorEventosPort;
import com.voltera.telemetriacore.domain.port.out.SerieRepositoryPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
public class BeanConfiguration {

    /** Proyector del lado QUERY (lámina 03: caja "Proyector · actualiza vistas"). */
    @Bean
    public ProyectorVista proyectorVista(ConsumoVistaRepositoryPort vistas,
                                         ConsumoAgregadoRepositoryPort vistasAgregadas) {
        return new ProyectorVista(vistas, vistasAgregadas);
    }

    @Bean
    public TelemetriaCoreService telemetriaCoreService(
            SerieRepositoryPort series,
            ConsumoVistaRepositoryPort vistas,
            ConsumoAgregadoRepositoryPort vistasAgregadas,
            ProyectorVista proyector,
            OutboxPort outbox,
            SerializadorEventosPort serializador,
            @Value("${telemetria-core.intervalo-min:15}") int intervaloMin,
            @Value("${telemetria-core.sin-reporte-min:30}") int sinReporteMin) {
        return new TelemetriaCoreService(series, vistas, vistasAgregadas, proyector,
                outbox, serializador, intervaloMin, sinReporteMin);
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper().registerModule(new JavaTimeModule());
    }
}
