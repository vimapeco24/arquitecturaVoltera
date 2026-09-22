package com.voltera.siniestros.application;

import com.voltera.siniestros.domain.model.EstadoSiniestro;
import com.voltera.siniestros.domain.model.MontoReclamacion;
import com.voltera.siniestros.domain.model.Siniestro;
import com.voltera.siniestros.domain.model.SiniestroAprobado;
import com.voltera.siniestros.domain.port.in.ReportarSiniestroUseCase.ComandoReportarSiniestro;
import com.voltera.siniestros.domain.port.out.EventPublisherPort;
import com.voltera.siniestros.domain.port.out.SiniestroRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("SiniestroService - casos de uso y maquina de estados")
class SiniestroServiceTest {

    private SiniestroRepositoryPort repositorio;
    private EventPublisherPort eventPublisher;
    private SiniestroService service;

    @BeforeEach
    void setUp() {
        repositorio = mock(SiniestroRepositoryPort.class);
        eventPublisher = mock(EventPublisherPort.class);
        service = new SiniestroService(repositorio, eventPublisher);
    }

    private ComandoReportarSiniestro comandoValido() {
        return new ComandoReportarSiniestro(
                "POL-001", "PRO-001", "Dano en inversor por sobretension",
                BigDecimal.valueOf(1500000), LocalDate.of(2026, 9, 1));
    }

    @Test
    @DisplayName("Reporta un siniestro nuevo y nace en estado REPORTADO")
    void reportaSiniestroNuevo() {
        when(repositorio.guardar(any(Siniestro.class))).thenAnswer(inv -> inv.getArgument(0));

        Siniestro s = service.reportar(comandoValido());

        assertEquals(EstadoSiniestro.REPORTADO, s.estado());
        verify(repositorio).guardar(any(Siniestro.class));
    }

    @Test
    @DisplayName("Aprueba un siniestro EN_PERITAJE, lo marca APROBADO y publica el evento")
    void apruebaSiniestroYPublicaEvento() {
        Siniestro siniestro = Siniestro.reportar(
                "POL-001", "PRO-001", "Dano en inversor",
                MontoReclamacion.de(1500000), LocalDate.of(2026, 9, 1));
        siniestro.enviarAPeritaje();
        when(repositorio.buscarPorId(any())).thenReturn(Optional.of(siniestro));
        when(repositorio.guardar(any(Siniestro.class))).thenAnswer(inv -> inv.getArgument(0));

        Siniestro aprobado = service.aprobar(siniestro.id().valor());

        assertEquals(EstadoSiniestro.APROBADO, aprobado.estado());
        verify(repositorio).guardar(any(Siniestro.class));
        verify(eventPublisher, times(1)).publicar(any(SiniestroAprobado.class));
    }

    @Test
    @DisplayName("Aprobar un siniestro ya APROBADO lanza IllegalStateException")
    void aprobarSiniestroYaAprobadoFalla() {
        Siniestro siniestro = Siniestro.reportar(
                "POL-001", "PRO-001", "Dano en inversor",
                MontoReclamacion.de(1500000), LocalDate.of(2026, 9, 1));
        siniestro.enviarAPeritaje();
        siniestro.aprobar(); // ya APROBADO
        when(repositorio.buscarPorId(any())).thenReturn(Optional.of(siniestro));

        assertThrows(IllegalStateException.class, () -> service.aprobar(siniestro.id().valor()));
        verify(eventPublisher, never()).publicar(any(SiniestroAprobado.class));
    }

    @Test
    @DisplayName("Reportar sin polizaId lanza IllegalArgumentException")
    void reportarSinPolizaFalla() {
        var comando = new ComandoReportarSiniestro(
                "  ", "PRO-001", "Dano en inversor",
                BigDecimal.valueOf(1500000), LocalDate.of(2026, 9, 1));

        assertThrows(IllegalArgumentException.class, () -> service.reportar(comando));
        verify(repositorio, never()).guardar(any(Siniestro.class));
    }
}
