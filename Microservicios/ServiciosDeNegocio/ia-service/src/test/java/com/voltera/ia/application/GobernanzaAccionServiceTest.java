package com.voltera.ia.application;

import com.voltera.ia.domain.model.Evidencia;
import com.voltera.ia.domain.model.PasoFlujo;
import com.voltera.ia.domain.model.der.DiagnosticoDer;
import com.voltera.ia.domain.model.der.Severidad;
import com.voltera.ia.domain.model.der.TipoAccion;
import com.voltera.ia.domain.model.der.TipoActivo;
import com.voltera.ia.domain.model.gobernanza.EstadoPropuesta;
import com.voltera.ia.domain.model.gobernanza.Firma;
import com.voltera.ia.domain.model.gobernanza.Firmante;
import com.voltera.ia.domain.model.gobernanza.PropuestaAccion;
import com.voltera.ia.domain.port.out.GobernanzaEventPublisherPort;
import com.voltera.ia.domain.port.out.McpBlockchainGatewayPort;
import com.voltera.ia.domain.port.out.NotificadorPort;
import com.voltera.ia.infrastructure.persistence.InMemoryPropuestaRepository;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Pruebas de la gobernanza multi-firma 3/3 (contrato MultiFirmaAccionDER). */
class GobernanzaAccionServiceTest {

    private final McpBlockchainGatewayPort gateway = new McpBlockchainGatewayPort() {
        @Override public String registrarPropuesta(PropuestaAccion p) { return "tx:prop"; }
        @Override public String registrarFirma(PropuestaAccion p, Firma f) { return "tx:firma"; }
        @Override public String ejecutarAccion(PropuestaAccion p) {
            if (p.estado() != EstadoPropuesta.APROBADA) {
                throw new IllegalStateException("fail-closed");
            }
            return "tx:exec";
        }
    };
    private final AtomicInteger notificaciones = new AtomicInteger();
    private final NotificadorPort notificador = new NotificadorPort() {
        @Override public String notificarFirmantePendiente(PropuestaAccion p, Firmante f) { notificaciones.incrementAndGet(); return "n"; }
        @Override public String escalar(PropuestaAccion p, Firmante f) { return "e"; }
        @Override public String notificarDesenlace(PropuestaAccion p) { return "d"; }
    };
    private final GobernanzaEventPublisherPort eventos = e -> true;

    private GobernanzaAccionService servicio(Duration ttl) {
        return new GobernanzaAccionService(gateway, notificador,
                new InMemoryPropuestaRepository(), eventos, ttl, ttl);
    }

    private DiagnosticoDer diagnosticoCritico() {
        return DiagnosticoDer.crear("INV-1", TipoActivo.INVERSOR, "COM-1", Severidad.CRITICA,
                "Inversor fuera de rango", TipoAccion.COMANDO_FISICO, "OT-INV-1 · PENDIENTE",
                true, List.of(Evidencia.de("RAG", "t", "d")),
                List.of(PasoFlujo.de(1, "n", "p", "d")), "local", false, "diagnosticador@v3", "op1");
    }

    @Test
    void propuestaNaceConFirma1DelAgente() {
        var s = servicio(Duration.ofMinutes(30));
        PropuestaAccion p = s.proponer(diagnosticoCritico());
        assertEquals(1, p.firmasAprobadas(), "la Firma 1 del agente queda aplicada al crear");
        assertEquals(EstadoPropuesta.PENDIENTE, p.estado());
        assertEquals("1/3", p.firmasResumen());
        assertTrue(notificaciones.get() >= 1, "se notifica al operador (siguiente firmante)");
    }

    @Test
    void soloConLasTresFirmasSeEjecuta() {
        var s = servicio(Duration.ofMinutes(30));
        PropuestaAccion p = s.proponer(diagnosticoCritico());

        s.aprobar(p.propuestaId(), Firmante.OPERADOR, "op@voltera", "validado técnicamente");
        assertEquals(EstadoPropuesta.PENDIENTE, p.estado(), "con 2/3 aún NO se ejecuta (no hay modo 2/3)");

        s.aprobar(p.propuestaId(), Firmante.PROSUMIDOR, "pro@casa", "autorizo");
        assertEquals(EstadoPropuesta.EJECUTADA, p.estado(), "con 3/3 se ejecuta");
        assertEquals(3, p.firmasAprobadas());
    }

    @Test
    void elRechazoDeUnFirmanteCierraLaPropuesta() {
        var s = servicio(Duration.ofMinutes(30));
        PropuestaAccion p = s.proponer(diagnosticoCritico());
        s.rechazar(p.propuestaId(), Firmante.PROSUMIDOR, "pro@casa", "no autorizo mi inversor");
        assertEquals(EstadoPropuesta.RECHAZADA, p.estado());
    }

    @Test
    void propuestaSinFirmarATiempoVence() throws InterruptedException {
        var s = servicio(Duration.ofMillis(20));   // TTL muy corto
        PropuestaAccion p = s.proponer(diagnosticoCritico());
        Thread.sleep(40);                           // deja vencer el TTL
        List<PropuestaAccion> vencidas = s.caducarVencidas();
        assertEquals(1, vencidas.size());
        assertEquals(EstadoPropuesta.VENCIDA, p.estado());
    }

    @Test
    void noSeProponeSiElDiagnosticoNoRequiereAccion() {
        var s = servicio(Duration.ofMinutes(30));
        DiagnosticoDer soloMonitoreo = DiagnosticoDer.crear("PAN-3", TipoActivo.PANEL_SOLAR, "COM-3",
                Severidad.INFO, "ok", TipoAccion.MONITOREAR, "OT · auto", false,
                List.of(), List.of(), "local", false, "diagnosticador@v3", "op3");
        assertThrows(IllegalArgumentException.class, () -> s.proponer(soloMonitoreo));
    }

    @Test
    void consultaInexistenteLanza() {
        var s = servicio(Duration.ofMinutes(30));
        assertThrows(IllegalArgumentException.class,
                () -> s.aprobar(UUID.randomUUID(), Firmante.OPERADOR, "x", "y"));
    }
}
