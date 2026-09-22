package com.voltera.liquidacion.application;

import com.voltera.liquidacion.domain.exception.TransaccionNoEncontradaException;
import com.voltera.liquidacion.domain.model.TransaccionId;
import com.voltera.liquidacion.domain.model.TransaccionP2P;
import com.voltera.liquidacion.domain.port.in.EmparejarOrdenesUseCase.ComandoEmparejar;
import com.voltera.liquidacion.domain.port.out.TransaccionRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("LiquidacionService - casos de uso")
class LiquidacionServiceTest {

    private TransaccionRepositoryPort repositorio;
    private LiquidacionService service;

    @BeforeEach
    void setUp() {
        repositorio = mock(TransaccionRepositoryPort.class);
        service = new LiquidacionService(repositorio);
    }

    @Test
    @DisplayName("Emparejar liquida y persiste la transaccion")
    void emparejarPersiste() {
        when(repositorio.guardar(any(TransaccionP2P.class))).thenAnswer(inv -> inv.getArgument(0));

        var comando = new ComandoEmparejar(
                "VENDEDOR", BigDecimal.valueOf(10), BigDecimal.valueOf(400),
                "COMPRADOR", BigDecimal.valueOf(10), BigDecimal.valueOf(500)
        );

        TransaccionP2P t = service.emparejar(comando);

        assertEquals("VENDEDOR", t.vendedorId());
        assertEquals("COMPRADOR", t.compradorId());
        assertEquals(new BigDecimal("450.00"), t.precioCasacion().porKwh());
        verify(repositorio, times(1)).guardar(any(TransaccionP2P.class));
    }

    @Test
    @DisplayName("Consultar por id inexistente lanza excepcion de dominio")
    void consultarInexistente() {
        when(repositorio.buscarPorId(any(TransaccionId.class))).thenReturn(Optional.empty());
        assertThrows(TransaccionNoEncontradaException.class, () -> service.porId("no-existe"));
    }

    @Test
    @DisplayName("Consultar por prosumidor delega en el repositorio")
    void consultarPorProsumidor() {
        when(repositorio.buscarPorProsumidor("PRO-1")).thenReturn(List.of());
        assertEquals(0, service.porProsumidor("PRO-1").size());
        verify(repositorio).buscarPorProsumidor("PRO-1");
    }
}
