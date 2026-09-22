package com.voltera.liquidacion.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Agregado TransaccionP2P - emparejamiento y liquidacion")
class TransaccionP2PTest {

    private OrdenMercado venta(String prosumidor, double kwh, double precio) {
        return OrdenMercado.crear(OrdenId.nuevo(), prosumidor, TipoOrden.VENTA,
                Energia.deKwh(kwh), Precio.porKwh(precio));
    }

    private OrdenMercado compra(String prosumidor, double kwh, double precio) {
        return OrdenMercado.crear(OrdenId.nuevo(), prosumidor, TipoOrden.COMPRA,
                Energia.deKwh(kwh), Precio.porKwh(precio));
    }

    @Test
    @DisplayName("Casa cantidades iguales al precio promedio")
    void casaCompleta() {
        OrdenMercado v = venta("VENDEDOR", 10, 400);
        OrdenMercado c = compra("COMPRADOR", 10, 500);

        TransaccionP2P t = TransaccionP2P.liquidar(TransaccionId.nuevo(), v, c);

        assertEquals(new BigDecimal("10.000"), t.energia().kwh());
        // precio de casacion = promedio(400, 500) = 450
        assertEquals(new BigDecimal("450.00"), t.precioCasacion().porKwh());
        // valor total = 10 * 450 = 4500
        assertEquals(new BigDecimal("4500.00"), t.valorTotal());
        assertTrue(v.estaCompleta());
        assertTrue(c.estaCompleta());
    }

    @Test
    @DisplayName("Casa el minimo cuando las cantidades difieren (casacion parcial)")
    void casaParcial() {
        OrdenMercado v = venta("VENDEDOR", 15, 400);
        OrdenMercado c = compra("COMPRADOR", 6, 500);

        TransaccionP2P t = TransaccionP2P.liquidar(TransaccionId.nuevo(), v, c);

        assertEquals(new BigDecimal("6.000"), t.energia().kwh());
        assertEquals(new BigDecimal("9.000"), v.pendiente().kwh()); // 15 - 6
        assertTrue(c.estaCompleta());
        // Resultado de casacion expuesto por el agregado
        assertEquals("PARCIAL", t.tipoCasacion());
        assertEquals(false, t.casacionTotal());
        assertEquals(new BigDecimal("9.000"), t.pendienteVenta().kwh());
        assertEquals(new BigDecimal("0.000"), t.pendienteCompra().kwh());
        assertEquals(new BigDecimal("15.000"), t.energiaSolicitadaVenta().kwh());
        assertEquals(new BigDecimal("6.000"), t.energiaSolicitadaCompra().kwh());
    }

    @Test
    @DisplayName("Rechaza casacion si el precio de venta supera al de compra")
    void rechazaPreciosIncompatibles() {
        OrdenMercado v = venta("VENDEDOR", 10, 600);
        OrdenMercado c = compra("COMPRADOR", 10, 500);
        assertThrows(IllegalArgumentException.class,
                () -> TransaccionP2P.liquidar(TransaccionId.nuevo(), v, c));
    }

    @Test
    @DisplayName("Rechaza que un prosumidor comercie consigo mismo")
    void rechazaAutocomercio() {
        OrdenMercado v = venta("MISMO", 10, 400);
        OrdenMercado c = compra("MISMO", 10, 500);
        assertThrows(IllegalArgumentException.class,
                () -> TransaccionP2P.liquidar(TransaccionId.nuevo(), v, c));
    }

    @Test
    @DisplayName("Rechaza si la primera orden no es de VENTA")
    void rechazaTipoIncorrecto() {
        OrdenMercado c1 = compra("A", 10, 500);
        OrdenMercado c2 = compra("B", 10, 500);
        assertThrows(IllegalArgumentException.class,
                () -> TransaccionP2P.liquidar(TransaccionId.nuevo(), c1, c2));
    }
}
