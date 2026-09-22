package com.voltera.liquidacionmensual.application;

import com.voltera.liquidacionmensual.domain.exception.LiquidacionNoEncontradaException;
import com.voltera.liquidacionmensual.domain.model.LiquidacionId;
import com.voltera.liquidacionmensual.domain.model.LiquidacionMensual;
import com.voltera.liquidacionmensual.domain.model.Periodo;
import com.voltera.liquidacionmensual.domain.port.in.GestionarLiquidacionUseCase.ComandoAbrir;
import com.voltera.liquidacionmensual.domain.port.in.GestionarLiquidacionUseCase.ComandoMovimiento;
import com.voltera.liquidacionmensual.domain.port.out.LiquidacionRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("LiquidacionMensualService - casos de uso")
class LiquidacionMensualServiceTest {

    private LiquidacionRepositoryPort repositorio;
    private LiquidacionMensualService service;

    @BeforeEach
    void setUp() {
        repositorio = mock(LiquidacionRepositoryPort.class);
        service = new LiquidacionMensualService(repositorio);
    }

    @Test
    @DisplayName("Abrir persiste una liquidacion ABIERTA")
    void abrir() {
        when(repositorio.guardar(any(LiquidacionMensual.class))).thenAnswer(inv -> inv.getArgument(0));
        LiquidacionMensual l = service.abrir(new ComandoAbrir("PRO-001", 2026, 1, null, null));
        assertEquals("PRO-001", l.prosumidorId());
        verify(repositorio).guardar(any(LiquidacionMensual.class));
    }

    @Test
    @DisplayName("Registrar movimiento acumula y persiste")
    void registrarMovimiento() {
        LiquidacionMensual existente = LiquidacionMensual.abrir("PRO-001", Periodo.de(2026, 1));
        when(repositorio.buscarPorId(any(LiquidacionId.class))).thenReturn(Optional.of(existente));
        when(repositorio.guardar(any(LiquidacionMensual.class))).thenAnswer(inv -> inv.getArgument(0));

        LiquidacionMensual l = service.registrarMovimiento(
                new ComandoMovimiento(existente.id().valor(), "CONSUMO", BigDecimal.valueOf(50)));

        assertEquals(new BigDecimal("50.000"), l.totalConsumo().kwh());
    }

    @Test
    @DisplayName("Consultar liquidacion inexistente lanza excepcion")
    void consultarInexistente() {
        when(repositorio.buscarPorId(any(LiquidacionId.class))).thenReturn(Optional.empty());
        assertThrows(LiquidacionNoEncontradaException.class, () -> service.porId("no-existe"));
    }
}
