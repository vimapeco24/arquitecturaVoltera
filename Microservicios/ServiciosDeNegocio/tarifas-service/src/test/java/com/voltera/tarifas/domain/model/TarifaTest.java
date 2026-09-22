package com.voltera.tarifas.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("Agregado Tarifa - franjas horarias")
class TarifaTest {

    @Test
    @DisplayName("Devuelve el precio de la franja que cubre la hora")
    void precioSegunFranja() {
        Tarifa t = Tarifa.crear("Residencial", List.of(
                FranjaHoraria.de(0, 18, Precio.porKwh(500)),   // valle
                FranjaHoraria.de(18, 22, Precio.porKwh(800)),  // pico
                FranjaHoraria.de(22, 24, Precio.porKwh(500))
        ));
        assertEquals(new BigDecimal("500.00"), t.precioEn(10).porKwh());
        assertEquals(new BigDecimal("800.00"), t.precioEn(19).porKwh());
    }

    @Test
    @DisplayName("Rechaza franjas que se solapan")
    void rechazaSolape() {
        assertThrows(IllegalArgumentException.class, () -> Tarifa.crear("Mala", List.of(
                FranjaHoraria.de(0, 12, Precio.porKwh(500)),
                FranjaHoraria.de(10, 18, Precio.porKwh(800))
        )));
    }

    @Test
    @DisplayName("Rechaza consultar una hora sin franja definida")
    void rechazaHoraSinFranja() {
        Tarifa t = Tarifa.crear("Parcial", List.of(
                FranjaHoraria.de(0, 12, Precio.porKwh(500))
        ));
        assertThrows(IllegalArgumentException.class, () -> t.precioEn(20));
    }

    @Test
    @DisplayName("Rechaza tarifa sin franjas")
    void rechazaSinFranjas() {
        assertThrows(IllegalArgumentException.class, () -> Tarifa.crear("Vacia", List.of()));
    }
}
