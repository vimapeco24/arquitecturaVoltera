package com.voltera.pagos.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("Agregado OrdenPago - maquina de estados")
class OrdenPagoTest {

    private OrdenPago pendiente() {
        return OrdenPago.crear("PRO-001", "FAC-100", TipoPago.COBRO_FACTURA, Monto.de(47600));
    }

    @Test
    @DisplayName("Una orden nace PENDIENTE")
    void nacePendiente() {
        assertEquals(EstadoPago.PENDIENTE, pendiente().estado());
    }

    @Test
    @DisplayName("Procesar cambia el estado a PROCESADO")
    void procesar() {
        OrdenPago o = pendiente();
        o.marcarProcesada();
        assertEquals(EstadoPago.PROCESADO, o.estado());
    }

    @Test
    @DisplayName("No se puede procesar dos veces")
    void noProcesarDosVeces() {
        OrdenPago o = pendiente();
        o.marcarProcesada();
        assertThrows(IllegalStateException.class, o::marcarProcesada);
    }

    @Test
    @DisplayName("Marcar fallida registra el motivo")
    void marcarFallida() {
        OrdenPago o = pendiente();
        o.marcarFallida("Fondos insuficientes");
        assertEquals(EstadoPago.FALLIDO, o.estado());
        assertEquals("Fondos insuficientes", o.motivoFallo());
    }

    @Test
    @DisplayName("Rechaza monto cero o negativo")
    void rechazaMontoInvalido() {
        assertThrows(IllegalArgumentException.class, () -> Monto.de(0));
        assertThrows(IllegalArgumentException.class, () -> Monto.de(-100));
    }

    @Test
    @DisplayName("Rechaza referencia vacia (clave de idempotencia)")
    void rechazaReferenciaVacia() {
        assertThrows(IllegalArgumentException.class,
                () -> OrdenPago.crear("PRO-001", "", TipoPago.COBRO_FACTURA, Monto.de(100)));
    }
}
