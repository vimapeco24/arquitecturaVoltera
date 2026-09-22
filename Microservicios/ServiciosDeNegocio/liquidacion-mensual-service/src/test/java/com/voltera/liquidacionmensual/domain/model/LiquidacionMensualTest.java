package com.voltera.liquidacionmensual.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Agregado LiquidacionMensual - totalizacion del mes")
class LiquidacionMensualTest {

    private LiquidacionMensual abierta() {
        return LiquidacionMensual.abrir("PRO-001", Periodo.de(2026, 1));
    }

    private Movimiento consumo(double kwh) {
        return Movimiento.de(Movimiento.Tipo.CONSUMO, Energia.deKwh(BigDecimal.valueOf(kwh)), Instant.now());
    }

    private Movimiento excedente(double kwh) {
        return Movimiento.de(Movimiento.Tipo.EXCEDENTE, Energia.deKwh(BigDecimal.valueOf(kwh)), Instant.now());
    }

    @Test
    @DisplayName("Nace ABIERTA sin movimientos")
    void naceAbierta() {
        LiquidacionMensual l = abierta();
        assertEquals(EstadoLiquidacion.ABIERTA, l.estado());
        assertEquals(0, l.cantidadMovimientos());
    }

    @Test
    @DisplayName("Totaliza consumos y excedentes y calcula el neto positivo")
    void totalizaNetoPositivo() {
        LiquidacionMensual l = abierta();
        l.registrarMovimiento(consumo(120));
        l.registrarMovimiento(consumo(30));
        l.registrarMovimiento(excedente(40));

        assertEquals(new BigDecimal("150.000"), l.totalConsumo().kwh());
        assertEquals(new BigDecimal("40.000"), l.totalExcedente().kwh());
        // neto = 150 - 40 = 110 (debe pagar)
        assertEquals(new BigDecimal("110.000"), l.netoKwh());
        assertFalse(l.tieneSaldoAFavor());
    }

    @Test
    @DisplayName("Neto negativo cuando el excedente supera el consumo (saldo a favor)")
    void netoSaldoAFavor() {
        LiquidacionMensual l = abierta();
        l.registrarMovimiento(consumo(20));
        l.registrarMovimiento(excedente(75));
        // neto = 20 - 75 = -55
        assertEquals(new BigDecimal("-55.000"), l.netoKwh());
        assertTrue(l.tieneSaldoAFavor());
    }

    @Test
    @DisplayName("Cerrar cambia el estado a CERRADA y fija fecha")
    void cerrar() {
        LiquidacionMensual l = abierta();
        l.registrarMovimiento(consumo(10));
        l.cerrar();
        assertEquals(EstadoLiquidacion.CERRADA, l.estado());
        assertTrue(l.cerradaEn() != null);
    }

    @Test
    @DisplayName("No se pueden registrar movimientos en una liquidacion CERRADA")
    void noRegistrarSiCerrada() {
        LiquidacionMensual l = abierta();
        l.cerrar();
        assertThrows(IllegalStateException.class, () -> l.registrarMovimiento(consumo(5)));
    }

    @Test
    @DisplayName("No se puede cerrar dos veces")
    void noCerrarDosVeces() {
        LiquidacionMensual l = abierta();
        l.cerrar();
        assertThrows(IllegalStateException.class, l::cerrar);
    }

    @Test
    @DisplayName("Rechaza mes invalido en el periodo")
    void rechazaMesInvalido() {
        assertThrows(IllegalArgumentException.class, () -> Periodo.de(2026, 13));
    }

    @Test
    @DisplayName("Valoracion economica con precios por defecto (680/420)")
    void valoracionPorDefecto() {
        LiquidacionMensual l = abierta();
        l.registrarMovimiento(consumo(100));
        l.registrarMovimiento(excedente(30));
        // cargo = 100 * 680 = 68000 ; credito = 30 * 420 = 12600 ; neto = 55400
        assertEquals(new BigDecimal("68000.00"), l.cargoConsumo());
        assertEquals(new BigDecimal("12600.00"), l.creditoExcedente());
        assertEquals(new BigDecimal("55400.00"), l.netoCop());
    }

    @Test
    @DisplayName("Valoracion economica con precios personalizados al abrir")
    void valoracionPreciosPersonalizados() {
        LiquidacionMensual l = LiquidacionMensual.abrir(
                "PRO-001", Periodo.de(2026, 1), new BigDecimal("500"), new BigDecimal("300"));
        l.registrarMovimiento(consumo(10));
        l.registrarMovimiento(excedente(4));
        // cargo = 10*500 = 5000 ; credito = 4*300 = 1200 ; neto = 3800
        assertEquals(new BigDecimal("5000.00"), l.cargoConsumo());
        assertEquals(new BigDecimal("1200.00"), l.creditoExcedente());
        assertEquals(new BigDecimal("3800.00"), l.netoCop());
    }
}
