package com.voltera.telemetriacore;

import com.voltera.telemetriacore.domain.port.out.ConsumoAgregadoRepositoryPort;
import com.voltera.telemetriacore.domain.port.out.ConsumoVistaRepositoryPort;
import com.voltera.telemetriacore.domain.port.out.SerieRepositoryPort;
import com.voltera.telemetriacore.infrastructure.persistence.InMemoryConsumoAgregadoRepository;
import com.voltera.telemetriacore.infrastructure.persistence.InMemoryConsumoVistaRepository;
import com.voltera.telemetriacore.infrastructure.persistence.InMemorySerieRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifica que, con el PERFIL POR DEFECTO (sin 'redis' ni 'tsdb'), el contexto
 * arranca sin requerir Redis ni base de datos y se cablean los adaptadores
 * in-memory. Kafka se deshabilita para el test (no hay broker).
 */
@SpringBootTest
@TestPropertySource(properties = {
        "spring.kafka.bootstrap-servers=localhost:9092",
        "spring.kafka.listener.auto-startup=false",
        "eureka.client.enabled=false"
})
@DisplayName("Arranque de contexto - perfil por defecto (in-memory)")
class ContextoPorDefectoTest {

    @Autowired SerieRepositoryPort serie;
    @Autowired ConsumoVistaRepositoryPort vista;
    @Autowired ConsumoAgregadoRepositoryPort agregado;

    @Test
    @DisplayName("Se inyectan los adaptadores in-memory y no los Redis/JDBC")
    void usaAdaptadoresInMemory() {
        assertInstanceOf(InMemorySerieRepository.class, serie);
        assertInstanceOf(InMemoryConsumoVistaRepository.class, vista);
        assertInstanceOf(InMemoryConsumoAgregadoRepository.class, agregado);
    }
}
