package com.voltera.liquidacion.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Entidad OrdenMercado")
class OrdenMercadoTest {

    @Test
    @DisplayName("Al crear, la energia pendiente es igual a la cantidad total")
    void pendienteInicial() {
        OrdenMercado o = OrdenMercado.crear(OrdenId.nuevo(), "PRO", TipoOrden.VENTA,
                Energia.deKwh(20), Precio.porKwh(300));
        assertEquals(new BigDecimal("20.000"), o.pendiente().kwh());
        assertFalse(o.estaCompleta());
    }

    @Test
    @DisplayName("Rechaza cantidad cero")
    void rechazaCantidadCero() {
        assertThrows(IllegalArgumentException.class,
                () -> OrdenMercado.crear(OrdenId.nuevo(), "PRO", TipoOrden.COMPRA,
                        Energia.cero(), Precio.porKwh(300)));
    }

    @Test
    @DisplayName("Rechaza prosumidor vacio")
    void rechazaProsumidorVacio() {
        assertThrows(IllegalArgumentException.class,
                () -> OrdenMercado.crear(OrdenId.nuevo(), "", TipoOrden.VENTA,
                        Energia.deKwh(5), Precio.porKwh(300)));
    }

    @Test
    @DisplayName("Queda completa cuando se casa toda su energia")
    void quedaCompleta() {
        OrdenMercado v = OrdenMercado.crear(OrdenId.nuevo(), "V", TipoOrden.VENTA,
                Energia.deKwh(10), Precio.porKwh(400));
        OrdenMercado c = OrdenMercado.crear(OrdenId.nuevo(), "C", TipoOrden.COMPRA,
                Energia.deKwh(10), Precio.porKwh(400));
        TransaccionP2P.liquidar(TransaccionId.nuevo(), v, c);
        assertTrue(v.estaCompleta());
    }
}
