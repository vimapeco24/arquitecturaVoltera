package com.voltera.tarifas.application;

import com.voltera.tarifas.domain.exception.TarifaNoEncontradaException;
import com.voltera.tarifas.domain.model.Tarifa;
import com.voltera.tarifas.domain.model.TarifaId;
import com.voltera.tarifas.domain.port.in.GestionarTarifaUseCase.ComandoCrearTarifa;
import com.voltera.tarifas.domain.port.in.GestionarTarifaUseCase.FranjaDTO;
import com.voltera.tarifas.domain.port.out.TarifaRepositoryPort;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("TarifaService - casos de uso")
class TarifaServiceTest {

    private TarifaRepositoryPort repositorio;
    private TarifaService service;

    @BeforeEach
    void setUp() {
        repositorio = mock(TarifaRepositoryPort.class);
        service = new TarifaService(repositorio);
    }

    @Test
    @DisplayName("Crear persiste la tarifa")
    void crearPersiste() {
        when(repositorio.guardar(any(Tarifa.class))).thenAnswer(inv -> inv.getArgument(0));
        var comando = new ComandoCrearTarifa("Residencial", List.of(
                new FranjaDTO(0, 18, BigDecimal.valueOf(500)),
                new FranjaDTO(18, 24, BigDecimal.valueOf(800))
        ));
        Tarifa t = service.crear(comando);
        assertEquals("Residencial", t.nombre());
        verify(repositorio).guardar(any(Tarifa.class));
    }

    @Test
    @DisplayName("Consultar tarifa inexistente lanza excepcion")
    void consultarInexistente() {
        when(repositorio.buscarPorId(any(TarifaId.class))).thenReturn(Optional.empty());
        assertThrows(TarifaNoEncontradaException.class, () -> service.porId("no-existe"));
    }
}
