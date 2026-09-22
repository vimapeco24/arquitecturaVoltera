package com.voltera.pagos.application;

import com.voltera.pagos.domain.model.EstadoPago;
import com.voltera.pagos.domain.model.Monto;
import com.voltera.pagos.domain.model.OrdenPago;
import com.voltera.pagos.domain.model.TipoPago;
import com.voltera.pagos.domain.port.in.ProcesarPagoUseCase.ComandoProcesarPago;
import com.voltera.pagos.domain.port.out.PagoRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("PagoService - casos de uso e idempotencia")
class PagoServiceTest {

    private PagoRepositoryPort repositorio;
    private PagoService service;

    @BeforeEach
    void setUp() {
        repositorio = mock(PagoRepositoryPort.class);
        service = new PagoService(repositorio);
    }

    @Test
    @DisplayName("Procesa un cobro nuevo y lo marca PROCESADO")
    void procesaCobroNuevo() {
        when(repositorio.buscarPorReferencia("FAC-100")).thenReturn(Optional.empty());
        when(repositorio.guardar(any(OrdenPago.class))).thenAnswer(inv -> inv.getArgument(0));

        var comando = new ComandoProcesarPago("PRO-001", "FAC-100", "COBRO_FACTURA", BigDecimal.valueOf(47600));
        OrdenPago o = service.procesar(comando);

        assertEquals(EstadoPago.PROCESADO, o.estado());
        verify(repositorio).guardar(any(OrdenPago.class));
    }

    @Test
    @DisplayName("Idempotencia: misma referencia devuelve la orden existente sin duplicar")
    void idempotencia() {
        OrdenPago existente = OrdenPago.crear("PRO-001", "FAC-100", TipoPago.COBRO_FACTURA, Monto.de(47600));
        when(repositorio.buscarPorReferencia("FAC-100")).thenReturn(Optional.of(existente));

        var comando = new ComandoProcesarPago("PRO-001", "FAC-100", "COBRO_FACTURA", BigDecimal.valueOf(47600));
        OrdenPago o = service.procesar(comando);

        assertEquals(existente.id(), o.id());
        verify(repositorio, never()).guardar(any(OrdenPago.class));
    }
}
