package com.voltera.facturacion.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Agregado Factura - reglas de negocio")
class FacturaTest {

    private final PeriodoFacturacion periodo =
            PeriodoFacturacion.de(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31));
    private final Tarifa tarifa =
            Tarifa.de(Dinero.de(680), Dinero.de(420));

    @Test
    @DisplayName("Emite factura con cargo cuando el prosumidor consume mas de lo que genera")
    void emiteFacturaConCargo() {
        ConsumoNeto consumo = ConsumoNeto.de(BigDecimal.valueOf(100), BigDecimal.valueOf(30));

        Factura factura = Factura.emitir(FacturaId.nuevo(), "PRO-001", periodo, consumo, tarifa);

        // consumo facturable = 70 kWh * 680 = 47.600 ; excedente = 0
        assertEquals(new BigDecimal("47600.00"), factura.cargoConsumo().valor());
        assertEquals(new BigDecimal("0.00"), factura.creditoExcedente().valor());
        assertEquals(new BigDecimal("47600.00"), factura.total().valor());
        assertFalse(factura.tieneSaldoAFavor());
        assertEquals(EstadoFactura.EMITIDA, factura.estado());
    }

    @Test
    @DisplayName("Emite factura con saldo a favor cuando genera mas de lo que consume")
    void emiteFacturaConSaldoAFavor() {
        ConsumoNeto consumo = ConsumoNeto.de(BigDecimal.valueOf(20), BigDecimal.valueOf(50));

        Factura factura = Factura.emitir(FacturaId.nuevo(), "PRO-002", periodo, consumo, tarifa);

        // excedente = 30 kWh * 420 = 12.600 ; consumo facturable = 0
        assertEquals(new BigDecimal("0.00"), factura.cargoConsumo().valor());
        assertEquals(new BigDecimal("12600.00"), factura.creditoExcedente().valor());
        assertEquals(new BigDecimal("-12600.00"), factura.total().valor());
        assertTrue(factura.tieneSaldoAFavor());
    }

    @Test
    @DisplayName("No permite emitir factura sin prosumidor")
    void rechazaProsumidorVacio() {
        ConsumoNeto consumo = ConsumoNeto.de(BigDecimal.TEN, BigDecimal.ZERO);
        assertThrows(IllegalArgumentException.class,
                () -> Factura.emitir(FacturaId.nuevo(), "  ", periodo, consumo, tarifa));
    }

    @Test
    @DisplayName("Solo una factura EMITIDA puede marcarse como PAGADA")
    void marcarPagadaDesdeEmitida() {
        Factura factura = facturaSimple();
        factura.marcarPagada();
        assertEquals(EstadoFactura.PAGADA, factura.estado());
    }

    @Test
    @DisplayName("No se puede pagar una factura ya anulada")
    void noPagarFacturaAnulada() {
        Factura factura = facturaSimple();
        factura.anular();
        assertThrows(IllegalStateException.class, factura::marcarPagada);
    }

    @Test
    @DisplayName("No se puede anular una factura ya pagada")
    void noAnularFacturaPagada() {
        Factura factura = facturaSimple();
        factura.marcarPagada();
        assertThrows(IllegalStateException.class, factura::anular);
    }

    private Factura facturaSimple() {
        ConsumoNeto consumo = ConsumoNeto.de(BigDecimal.valueOf(50), BigDecimal.ZERO);
        return Factura.emitir(FacturaId.nuevo(), "PRO-003", periodo, consumo, tarifa);
    }
}
