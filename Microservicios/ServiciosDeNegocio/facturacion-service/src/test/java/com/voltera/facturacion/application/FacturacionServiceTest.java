package com.voltera.facturacion.application;

import com.voltera.facturacion.domain.exception.FacturaNoEncontradaException;
import com.voltera.facturacion.domain.model.Factura;
import com.voltera.facturacion.domain.model.FacturaId;
import com.voltera.facturacion.domain.port.in.EmitirFacturaUseCase.ComandoEmitirFactura;
import com.voltera.facturacion.domain.port.out.FacturaRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("FacturacionService - casos de uso")
class FacturacionServiceTest {

    private FacturaRepositoryPort repositorio;
    private FacturacionService service;

    @BeforeEach
    void setUp() {
        repositorio = mock(FacturaRepositoryPort.class);
        service = new FacturacionService(repositorio);
    }

    @Test
    @DisplayName("Emitir persiste la factura y devuelve el agregado calculado")
    void emitirPersiste() {
        when(repositorio.guardar(any(Factura.class))).thenAnswer(inv -> inv.getArgument(0));

        var comando = new ComandoEmitirFactura(
                "PRO-001",
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 1, 31),
                BigDecimal.valueOf(100),
                BigDecimal.valueOf(30),
                BigDecimal.valueOf(680),
                BigDecimal.valueOf(420)
        );

        Factura resultado = service.emitir(comando);

        assertEquals("PRO-001", resultado.prosumidorId());
        assertEquals(new BigDecimal("47600.00"), resultado.total().valor());
        verify(repositorio, times(1)).guardar(any(Factura.class));
    }

    @Test
    @DisplayName("Consultar por id inexistente lanza excepcion de dominio")
    void consultarInexistente() {
        when(repositorio.buscarPorId(any(FacturaId.class))).thenReturn(Optional.empty());
        assertThrows(FacturaNoEncontradaException.class, () -> service.porId("no-existe"));
    }

    @Test
    @DisplayName("Consultar por prosumidor delega en el repositorio")
    void consultarPorProsumidor() {
        when(repositorio.buscarPorProsumidor("PRO-9")).thenReturn(List.of());
        List<Factura> res = service.porProsumidor("PRO-9");
        assertEquals(0, res.size());
        verify(repositorio).buscarPorProsumidor("PRO-9");
    }
}
