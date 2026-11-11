package com.voltera.ia.application;

import com.voltera.ia.domain.model.Decision;
import com.voltera.ia.domain.model.Evidencia;
import com.voltera.ia.domain.model.IaRecomendacionGenerada;
import com.voltera.ia.domain.model.RecomendacionCopiloto;
import com.voltera.ia.domain.model.SolicitudCopiloto;
import com.voltera.ia.domain.port.out.ConocimientoPort;
import com.voltera.ia.domain.port.out.EventPublisherPort;
import com.voltera.ia.domain.port.out.GuardrailsPort;
import com.voltera.ia.domain.port.out.HerramientasPort;
import com.voltera.ia.domain.port.out.ModelGatewayPort;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas del flujo agentico del Copiloto de Siniestros con dobles de prueba de
 * los puertos de salida (sin infraestructura real).
 */
class CopilotoSiniestrosServiceTest {

    private final BigDecimal umbral = new BigDecimal("3000000");

    private final GuardrailsPort guardrailsNoop = new GuardrailsPort() {
        @Override public String enmascararPII(String texto) { return texto == null ? "" : texto; }
        @Override public void validarEntrada(String texto) { /* permite todo */ }
    };

    private final ModelGatewayPort gatewayLocal = new ModelGatewayPort() {
        @Override public Optional<String> redactarJustificacion(SolicitudCopiloto s, List<Evidencia> e) { return Optional.empty(); }
        @Override public boolean proveedorActivo() { return false; }
        @Override public String identificadorMotor() { return "local"; }
    };

    private CopilotoSiniestrosService servicio(ConocimientoPort rag, HerramientasPort tools,
                                               EventPublisherPort pub) {
        return new CopilotoSiniestrosService(rag, tools, gatewayLocal, guardrailsNoop, pub, umbral);
    }

    @Test
    void montoAltoExigeRevisionHumana() {
        AtomicReference<IaRecomendacionGenerada> publicado = new AtomicReference<>();
        var servicio = servicio(
                s -> List.of(Evidencia.de("RAG:polizas", "cobertura", "ok")),
                s -> List.of(
                        Evidencia.de("tool:siniestros-service", "estado", "APROBADO"),
                        Evidencia.de("chain:blockchain-gateway", "riesgo cedido", "ok")),
                evento -> { publicado.set(evento); return true; });

        var req = new SolicitudCopiloto("SIN-1", "POL-1", "PRO-1",
                "Daño por sobretension", new BigDecimal("9000000"), true);
        RecomendacionCopiloto r = servicio.analizar(req);

        assertTrue(r.requiereRevisionHumana(), "monto > umbral debe exigir humano");
        assertEquals(Decision.ESCALAR_HUMANO, r.decisionPropuesta());
        assertEquals("local", r.motor());
        assertNotNull(publicado.get(), "debe publicar el evento IaRecomendacionGenerada");
        assertEquals("SIN-1", publicado.get().siniestroId());
        // El flujo tiene los 7 nodos + 1 de guardrails = 8 pasos de traza.
        assertEquals(8, r.traza().size());
    }

    @Test
    void casoSimpleConEvidenciaCompletaSePuedeAprobar() {
        var servicio = servicio(
                s -> List.of(Evidencia.de("RAG:polizas", "cobertura", "ok")),
                s -> List.of(
                        Evidencia.de("tool:siniestros-service", "estado", "EN_PERITAJE"),
                        Evidencia.de("chain:blockchain-gateway", "riesgo cedido", "ok")),
                evento -> true);

        var req = new SolicitudCopiloto("SIN-2", "POL-2", "PRO-2",
                "Falla menor", new BigDecimal("500000"), false);
        RecomendacionCopiloto r = servicio.analizar(req);

        assertFalse(r.requiereRevisionHumana(), "monto bajo + confianza alta no exige humano");
        assertEquals(Decision.APROBAR, r.decisionPropuesta());
        assertEquals(new BigDecimal("500000"), r.montoSugerido());
    }

    @Test
    void sinHerramientasSolicitaInformacion() {
        var servicio = servicio(
                s -> List.of(Evidencia.de("RAG:polizas", "cobertura", "ok")),
                s -> List.of(), // sin evidencia de herramientas
                evento -> true);

        var req = new SolicitudCopiloto("SIN-3", "POL-3", "PRO-3",
                "Caso sin verificacion", new BigDecimal("100000"), false);
        RecomendacionCopiloto r = servicio.analizar(req);

        assertEquals(Decision.SOLICITAR_INFO, r.decisionPropuesta());
    }
}
