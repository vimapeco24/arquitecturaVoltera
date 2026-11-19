package com.voltera.liquidacion.infrastructure;

import com.voltera.liquidacion.domain.model.LiquidacionCalculada;
import com.voltera.liquidacion.infrastructure.messaging.NoopLiquidacionEventPublisher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertFalse;

@DisplayName("NoopLiquidacionEventPublisher")
class NoopLiquidacionEventPublisherTest {

    @Test
    @DisplayName("No publica y devuelve false")
    void noPublica() {
        var e = new LiquidacionCalculada("e", "TX", "V", "C", 1L, BigDecimal.ONE, BigDecimal.ONE, Instant.now(), null);
        assertFalse(new NoopLiquidacionEventPublisher().publicar(e));
    }
}
