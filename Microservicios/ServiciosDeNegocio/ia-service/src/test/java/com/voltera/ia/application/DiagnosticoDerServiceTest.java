package com.voltera.ia.application;

import com.voltera.ia.domain.model.Evidencia;
import com.voltera.ia.domain.model.SolicitudCopiloto;
import com.voltera.ia.domain.model.der.AlertaTelemetria;
import com.voltera.ia.domain.model.der.DiagnosticoDer;
import com.voltera.ia.domain.model.der.DiagnosticoDerGenerado;
import com.voltera.ia.domain.model.der.Severidad;
import com.voltera.ia.domain.model.der.TipoAccion;
import com.voltera.ia.domain.model.der.TipoActivo;
import com.voltera.ia.domain.port.out.AgentRegistryPort;
import com.voltera.ia.domain.port.out.AuditLogPort;
import com.voltera.ia.domain.port.out.ConocimientoDerPort;
import com.voltera.ia.domain.port.out.DiagnosticoEventPublisherPort;
import com.voltera.ia.domain.port.out.GuardrailsPort;
import com.voltera.ia.domain.port.out.HerramientasDerPort;
import com.voltera.ia.domain.port.out.ModelGatewayPort;
import com.voltera.ia.domain.port.out.PromptModelRegistryPort;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Pruebas del flujo de 3 agentes del diagnóstico DER (caso Grupo 5). */
class DiagnosticoDerServiceTest {

    private final GuardrailsPort guardrails = new GuardrailsPort() {
        @Override public String enmascararPII(String t) { return t == null ? "" : t; }
        @Override public void validarEntrada(String t) { }
    };
    private final ModelGatewayPort gatewayLocal = new ModelGatewayPort() {
        @Override public Optional<String> redactarJustificacion(SolicitudCopiloto s, List<Evidencia> e) { return Optional.empty(); }
        @Override public boolean proveedorActivo() { return false; }
        @Override public String identificadorMotor() { return "local"; }
    };
    private final PromptModelRegistryPort prompts = new PromptModelRegistryPort() {
        @Override public String versionPrompt(String a) { return a + "@test"; }
        @Override public String promptActivo(String a) { return ""; }
    };
    private final AgentRegistryPort registry = new AgentRegistryPort() {
        @Override public List<AgenteRegistrado> agentesActivos() {
            return List.of(new AgenteRegistrado("Agente-Monitor", "x", "y", "z", true));
        }
        @Override public boolean habilitado(String n) { return true; }
    };

    private DiagnosticoDerService servicio(ConocimientoDerPort rag, HerramientasDerPort tools,
                                           AuditLogPort audit, DiagnosticoEventPublisherPort pub) {
        return new DiagnosticoDerService(rag, tools, registry, guardrails, gatewayLocal, prompts, audit, pub);
    }

    @Test
    void inversorFueraDeRangoEsCriticoYExigeAprobacion() {
        AtomicReference<DiagnosticoDerGenerado> evento = new AtomicReference<>();
        AtomicReference<DiagnosticoDer> auditado = new AtomicReference<>();
        var servicio = servicio(
                a -> List.of(Evidencia.de("RAG:ficha-tecnica", "t", "d")),
                a -> List.of(Evidencia.de("tool:telemetria", "t", "d")),
                auditado::set,
                e -> { evento.set(e); return true; });

        var alerta = new AlertaTelemetria("INV-1", TipoActivo.INVERSOR, "COM-1",
                "voltaje", new BigDecimal("260"), new BigDecimal("240"), 0, "fuera de rango", "op1");
        DiagnosticoDer d = servicio.diagnosticar(alerta);

        assertEquals(Severidad.CRITICA, d.severidad());
        assertEquals(TipoAccion.COMANDO_FISICO, d.accionPropuesta());
        assertTrue(d.requiereAprobacionHumana(), "comando físico exige Human Review");
        assertEquals("local", d.motor());
        assertNotNull(evento.get());
        assertNotNull(auditado.get(), "debe registrarse en auditoría (caja 8)");
        assertEquals("op1", d.usuario(), "la identidad del operador debe propagarse (IAM)");
    }

    @Test
    void medidorSinSenalDespachaTecnico() {
        var servicio = servicio(
                a -> List.of(Evidencia.de("RAG:ficha-tecnica", "t", "d")),
                a -> List.of(Evidencia.de("tool:telemetria", "t", "d")),
                d -> {},
                e -> true);

        var alerta = new AlertaTelemetria("MED-9", TipoActivo.MEDIDOR, "COM-2",
                "latencia", BigDecimal.ZERO, BigDecimal.ZERO, 30, "sin señal", "op2");
        DiagnosticoDer d = servicio.diagnosticar(alerta);

        assertEquals(TipoAccion.DESPACHAR_TECNICO, d.accionPropuesta());
        assertTrue(d.requiereAprobacionHumana());
    }

    @Test
    void metricaDentroDeRangoSoloMonitorea() {
        var servicio = servicio(
                a -> List.of(Evidencia.de("RAG:ficha-tecnica", "t", "d")),
                a -> List.of(Evidencia.de("tool:telemetria", "t", "d")),
                d -> {},
                e -> true);

        var alerta = new AlertaTelemetria("PAN-3", TipoActivo.PANEL_SOLAR, "COM-3",
                "generacion", new BigDecimal("100"), new BigDecimal("120"), 0, "normal", "op3");
        DiagnosticoDer d = servicio.diagnosticar(alerta);

        assertEquals(Severidad.INFO, d.severidad());
        assertEquals(TipoAccion.MONITOREAR, d.accionPropuesta());
        assertFalse(d.requiereAprobacionHumana());
    }
}
