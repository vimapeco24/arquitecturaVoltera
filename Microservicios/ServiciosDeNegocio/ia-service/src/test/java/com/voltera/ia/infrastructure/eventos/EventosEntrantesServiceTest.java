package com.voltera.ia.infrastructure.eventos;

import com.voltera.ia.domain.model.der.AlertaTelemetria;
import com.voltera.ia.domain.model.der.DiagnosticoDer;
import com.voltera.ia.domain.model.der.TipoActivo;
import com.voltera.ia.domain.port.in.DiagnosticoDerUseCase;
import com.voltera.ia.infrastructure.audit.AuditoriaIa;
import com.voltera.ia.infrastructure.knowledge.Bm25ConocimientoDer;
import com.voltera.ia.infrastructure.observabilidad.ObservabilidadIa;
import com.voltera.ia.infrastructure.revision.RegistroAlertas;
import com.voltera.ia.infrastructure.tools.McpNegocioAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Disparo por evento del diagnóstico, mantenimiento del RAG por evento y herramientas de negocio. */
class EventosEntrantesServiceTest {

    private DiagnosticoDerUseCase diag;
    private Bm25ConocimientoDer rag;
    private EventosEntrantesService svc;

    @BeforeEach
    void init() {
        diag = mock(DiagnosticoDerUseCase.class);
        DiagnosticoDer d = DiagnosticoDer.crear("MED-9", TipoActivo.MEDIDOR, "c1",
                com.voltera.ia.domain.model.der.Severidad.ADVERTENCIA, "x",
                com.voltera.ia.domain.model.der.TipoAccion.DESPACHAR_TECNICO, "ot", true,
                java.util.List.of(), java.util.List.of(), "local", false, "v", "u");
        when(diag.diagnosticar(any(AlertaTelemetria.class))).thenReturn(d);
        rag = new Bm25ConocimientoDer();
        svc = new EventosEntrantesService(diag, new RegistroAlertas(),
                new ObservabilidadIa(new io.micrometer.core.instrument.simple.SimpleMeterRegistry()),
                rag, new AuditoriaIa(new com.voltera.ia.infrastructure.guardrails.RegexGuardrails(), 365, 5000));
    }

    private static final String SIN_REPORTE = "{\"medidorSerial\":\"MED-9\",\"ultimaLecturaEn\":\"2026-01-01T10:00:00Z\","
            + "\"detectadoEn\":\"2026-01-01T10:45:00Z\"}";

    @Test
    void medidorSinReporteDisparaElDiagnosticoComoMedidorConMinutosSinSenal() {
        assertTrue(svc.procesarAlerta(SIN_REPORTE).isPresent());
        var cap = ArgumentCaptor.forClass(AlertaTelemetria.class);
        verify(diag).diagnosticar(cap.capture());
        assertEquals("MED-9", cap.getValue().activoId());
        assertEquals(TipoActivo.MEDIDOR, cap.getValue().tipoActivo());
        assertEquals(45, cap.getValue().minutosSinSenal());
        assertTrue(cap.getValue().sinSenal());
        assertEquals("evento:telemetria-core", cap.getValue().usuario());
    }

    @Test
    void eventoDuplicadoOMalformadoNoDisparaOtroDiagnostico() {
        svc.procesarAlerta(SIN_REPORTE);
        assertTrue(svc.procesarAlerta(SIN_REPORTE).isEmpty());
        assertTrue(svc.procesarAlerta("no es json").isEmpty());
        assertTrue(svc.procesarAlerta("{\"otro\":1}").isEmpty());
        verify(diag, times(1)).diagnosticar(any(AlertaTelemetria.class));
    }

    @Test
    void conocimientoPublicadoQuedaBuscableYRetiradoSale() {
        int antes = rag.documentos().size();
        String contenido = "id: zeta-fallos\\ntitulo: Ficha Zeta\\ncategoria: fallos-conocidos\\nactivos: INVERSOR\\n---\\n## Fallo de prueba\\nzumbido zafiro irrepetible del inversor";
        var r = svc.procesarConocimiento("{\"tipo\":\"DocumentoConocimientoPublicado\",\"contenido\":\"" + contenido + "\"}");
        assertTrue(r.isPresent());
        assertEquals(antes + 1, rag.documentos().size());
        String id = (String) r.get().get("documentoId");
        var ret = svc.procesarConocimiento("{\"tipo\":\"DocumentoConocimientoRetirado\",\"documentoId\":\"" + id + "\"}");
        assertEquals(true, ret.get().get("retirado"));
        assertEquals(antes, rag.documentos().size());
        assertTrue(svc.procesarConocimiento("{\"tipo\":\"Otro\"}").isEmpty());
    }

    @Test
    void herramientasDeNegocioDegradanSinFallarCuandoElServicioNoResponde() {
        var a = new McpNegocioAdapter(true, 300, "http://localhost:9", "http://localhost:9",
                "http://localhost:9", "http://localhost:9", WebClient.builder());
        var ev = a.consultar(new AlertaTelemetria("INV-1", TipoActivo.INVERSOR, "c1", "voltaje",
                BigDecimal.TEN, BigDecimal.ONE, 0, "", "op"));
        assertEquals(4, ev.size());
        assertTrue(ev.stream().anyMatch(e -> e.fuente().equals("tool:certificados-renovables")));
        assertTrue(ev.stream().allMatch(e -> e.detalle().contains("no verificable")));
    }
}
