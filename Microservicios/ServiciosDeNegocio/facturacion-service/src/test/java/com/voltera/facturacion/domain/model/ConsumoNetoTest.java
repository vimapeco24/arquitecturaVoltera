package com.voltera.facturacion.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("Value Object ConsumoNeto")
class ConsumoNetoTest {

    @Test
    @DisplayName("Calcula consumo facturable cuando consume mas de lo que inyecta")
    void consumoFacturable() {
        ConsumoNeto c = ConsumoNeto.de(BigDecimal.valueOf(120), BigDecimal.valueOf(40));
        assertEquals(BigDecimal.valueOf(80), c.consumoFacturable());
        assertEquals(BigDecimal.ZERO, c.excedente());
    }

    @Test
    @DisplayName("Calcula excedente cuando inyecta mas de lo que consume")
    void excedente() {
        ConsumoNeto c = ConsumoNeto.de(BigDecimal.valueOf(30), BigDecimal.valueOf(75));
        assertEquals(BigDecimal.valueOf(45), c.excedente());
        assertEquals(BigDecimal.ZERO, c.consumoFacturable());
    }

    @Test
    @DisplayName("Rechaza valores negativos")
    void rechazaNegativos() {
        assertThrows(IllegalArgumentException.class,
                () -> ConsumoNeto.de(BigDecimal.valueOf(-1), BigDecimal.ZERO));
        assertThrows(IllegalArgumentException.class,
                () -> ConsumoNeto.de(BigDecimal.ZERO, BigDecimal.valueOf(-5)));
    }
}
