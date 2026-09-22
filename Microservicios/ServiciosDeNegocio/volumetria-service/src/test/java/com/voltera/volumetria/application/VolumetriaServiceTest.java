package com.voltera.volumetria.application;

import com.voltera.volumetria.domain.exception.LecturaNoEncontradaException;
import com.voltera.volumetria.domain.model.EstadoLectura;
import com.voltera.volumetria.domain.model.LecturaId;
import com.voltera.volumetria.domain.model.LecturaTelemetria;
import com.voltera.volumetria.domain.port.in.IngestarLecturaUseCase.ComandoIngestarLectura;
import com.voltera.volumetria.domain.port.out.LecturaRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("VolumetriaService - casos de uso")
class VolumetriaServiceTest {

    private LecturaRepositoryPort repositorio;
    private VolumetriaService service;

    @BeforeEach
    void setUp() {
        repositorio = mock(LecturaRepositoryPort.class);
        service = new VolumetriaService(repositorio);
    }

    @Test
    @DisplayName("Ingesta una lectura de consumo y la persiste")
    void ingestaPersiste() {
        when(repositorio.guardar(any(LecturaTelemetria.class))).thenAnswer(inv -> inv.getArgument(0));
        var comando = new ComandoIngestarLectura("MED-001", BigDecimal.valueOf(2.5),
                "CONSUMO", Instant.now().minus(1, ChronoUnit.MINUTES));
        LecturaTelemetria l = service.ingestar(comando);
        assertEquals("MED-001", l.medidorId().valor());
        assertEquals(EstadoLectura.VALIDA, l.estado());
        verify(repositorio).guardar(any(LecturaTelemetria.class));
    }

    @Test
    @DisplayName("Consultar lectura inexistente lanza excepcion")
    void consultarInexistente() {
        when(repositorio.buscarPorId(any(LecturaId.class))).thenReturn(Optional.empty());
        assertThrows(LecturaNoEncontradaException.class, () -> service.porId("no-existe"));
    }
}
