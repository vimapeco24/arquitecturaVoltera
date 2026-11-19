package com.voltera.ia.application;

import com.voltera.ia.domain.model.Evidencia;
import com.voltera.ia.domain.model.PasoFlujo;
import com.voltera.ia.domain.model.der.DiagnosticoDer;
import com.voltera.ia.domain.model.der.Severidad;
import com.voltera.ia.domain.model.der.TipoAccion;
import com.voltera.ia.domain.model.der.TipoActivo;
import com.voltera.ia.domain.model.gobernanza.EstadoAprobaciones;
import com.voltera.ia.domain.model.gobernanza.Firma;
import com.voltera.ia.domain.model.gobernanza.Firmante;
import com.voltera.ia.domain.model.gobernanza.LecturaOnChain;
import com.voltera.ia.domain.model.gobernanza.PropuestaAccion;
import com.voltera.ia.domain.port.out.McpBlockchainGatewayPort;
import com.voltera.ia.domain.port.out.NotificadorPort;
import com.voltera.ia.infrastructure.persistence.InMemoryPropuestaRepository;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Vista de lectura de las aprobaciones múltiples: 3/3, 2/3, rechazo, simulado y on-chain (cliente mockeado). */
class GobernanzaAprobacionesTest {

    private static final String BASE = "https://amoy.polygonscan.com";

    /** Gateway de prueba: devuelve hashes reales (0x + 64 hex) o referencias demo, y una lectura configurable. */
    private static class GatewayFalso implements McpBlockchainGatewayPort {
        private final boolean hashesReales;
        private final LecturaOnChain lectura;
        private final AtomicInteger n = new AtomicInteger();

        GatewayFalso(boolean hashesReales, LecturaOnChain lectura) {
            this.hashesReales = hashesReales;
            this.lectura = lectura;
        }

        private String tx(String tipo) {
            int i = n.incrementAndGet();
            return hashesReales ? "0x" + String.format("%064x", i) : "demo:" + tipo + ":" + i;
        }

        @Override public String registrarPropuesta(PropuestaAccion p) { return tx("propuesta"); }
        @Override public String registrarFirma(PropuestaAccion p, Firma f) { return tx("firma"); }
        @Override public String ejecutarAccion(PropuestaAccion p) { return tx("ejecucion"); }
        @Override public LecturaOnChain leerEstado(PropuestaAccion p) {
            return lectura != null ? lectura : McpBlockchainGatewayPort.super.leerEstado(p);
        }
    }

    private final NotificadorPort notificador = new NotificadorPort() {
        @Override public String notificarFirmantePendiente(PropuestaAccion p, Firmante f) { return "n"; }
        @Override public String escalar(PropuestaAccion p, Firmante f) { return "e"; }
        @Override public String notificarDesenlace(PropuestaAccion p) { return "d"; }
    };

    private GobernanzaAccionService servicio(McpBlockchainGatewayPort gw) {
        return new GobernanzaAccionService(gw, notificador, new InMemoryPropuestaRepository(),
                e -> true, Duration.ofMinutes(30), Duration.ofMinutes(30), BASE + "/");
    }

    private DiagnosticoDer diagnostico() {
        return DiagnosticoDer.crear("INV-1", TipoActivo.INVERSOR, "COM-1", Severidad.CRITICA,
                "Inversor fuera de rango", TipoAccion.COMANDO_FISICO, "OT-INV-1 · PENDIENTE",
                true, List.of(Evidencia.de("RAG", "t", "d")),
                List.of(PasoFlujo.de(1, "n", "p", "d")), "local", false, "diagnosticador@v3", "op1");
    }

    private EstadoAprobaciones estado(GobernanzaAccionService s, PropuestaAccion p) {
        return s.aprobaciones(p.propuestaId()).orElseThrow();
    }

    @Test
    void tresDeTresCompletoMuestraTodasLasFirmasConTxYEnlace() {
        var s = servicio(new GatewayFalso(true, null));
        PropuestaAccion p = s.proponer(diagnostico());
        s.aprobar(p.propuestaId(), Firmante.OPERADOR, "op@voltera", "ok");
        s.aprobar(p.propuestaId(), Firmante.PROSUMIDOR, "pro@casa", "autorizo");

        EstadoAprobaciones e = estado(s, p);
        assertEquals("EJECUTADA", e.estado());
        assertTrue(e.estadoGlobal().contains("3-de-3"));
        assertEquals("3/3", e.progreso());
        assertEquals(3, e.firmasAprobadas());
        assertEquals(3, e.firmasRequeridas());
        assertTrue(e.faltantes().isEmpty());
        assertEquals(3, e.firmantes().size());
        for (var f : e.firmantes()) {
            assertEquals("FIRMO", f.estado());
            assertNotNull(f.firmadoEn());
            assertTrue(f.txHash().matches("^0x[0-9a-f]{64}$"));
            assertEquals(BASE + "/tx/" + f.txHash(), f.explorerUrl());
            assertEquals("ON_CHAIN", f.modo());
        }
        assertEquals("ON_CHAIN", e.modo());
        assertNotNull(e.txEjecucionUrl());
    }

    @Test
    void dosDeTresQuedaPendienteYListaLaFirmaFaltante() {
        var s = servicio(new GatewayFalso(true, null));
        PropuestaAccion p = s.proponer(diagnostico());
        s.aprobar(p.propuestaId(), Firmante.OPERADOR, "op@voltera", "ok");

        EstadoAprobaciones e = estado(s, p);
        assertEquals("PENDIENTE", e.estado());
        assertEquals("2/3", e.progreso());
        assertEquals(List.of("PROSUMIDOR"), e.faltantes());
        var pros = e.firmantes().stream().filter(f -> f.firmante().equals("PROSUMIDOR")).findFirst().orElseThrow();
        assertEquals("PENDIENTE", pros.estado());
        assertNull(pros.txHash());
        assertNull(pros.firmadoEn());
        assertNull(e.txEjecucion(), "con 2/3 no hay ejecución");
    }

    @Test
    void rechazoMarcaRechazoYNoEjecuta() {
        var s = servicio(new GatewayFalso(true, null));
        PropuestaAccion p = s.proponer(diagnostico());
        s.aprobar(p.propuestaId(), Firmante.OPERADOR, "op@voltera", "ok");
        s.rechazar(p.propuestaId(), Firmante.PROSUMIDOR, "pro@casa", "no autorizo");

        EstadoAprobaciones e = estado(s, p);
        assertEquals("RECHAZADA", e.estado());
        assertTrue(e.estadoGlobal().startsWith("RECHAZADA"));
        var pros = e.firmantes().stream().filter(f -> f.firmante().equals("PROSUMIDOR")).findFirst().orElseThrow();
        assertEquals("RECHAZO", pros.estado());
        assertEquals("no autorizo", pros.razon());
        assertTrue(e.faltantes().isEmpty(), "una propuesta cerrada no tiene firmas por esperar");
        assertNull(e.txEjecucion());
    }

    @Test
    void modoSimuladoNoFallaYNoInventaEnlaces() {
        var s = servicio(new GatewayFalso(false, null));
        PropuestaAccion p = s.proponer(diagnostico());
        s.aprobar(p.propuestaId(), Firmante.OPERADOR, "op@voltera", "ok");

        EstadoAprobaciones e = estado(s, p);
        assertEquals("SIMULADO", e.modo());
        assertEquals("SIMULADO", e.lecturaOnChain().modo());
        for (var f : e.firmantes().stream().filter(f -> f.txHash() != null).toList()) {
            assertTrue(f.txHash().startsWith("demo:"));
            assertNull(f.explorerUrl());
            assertEquals("SIMULADO", f.modo());
        }
    }

    @Test
    void modoOnChainConLecturaMockeadaUsaLosHashesDelContrato() {
        String hashOperador = "0x" + "ab".repeat(32);
        var lectura = new LecturaOnChain("ON_CHAIN", true, "PENDIENTE", 2,
                List.of(new LecturaOnChain.FirmaOnChain("OPERADOR", "0xOperador", hashOperador,
                        Instant.parse("2026-11-16T10:00:00Z"))),
                "0xContrato", "polygon-amoy", null);
        // El gateway de escritura devuelve referencias demo, pero la lectura on-chain trae el hash real.
        var s = servicio(new GatewayFalso(false, lectura));
        PropuestaAccion p = s.proponer(diagnostico());
        s.aprobar(p.propuestaId(), Firmante.OPERADOR, "op@voltera", "ok");

        EstadoAprobaciones e = estado(s, p);
        assertEquals("ON_CHAIN", e.modo());
        assertEquals("0xContrato", e.contrato());
        var op = e.firmantes().stream().filter(f -> f.firmante().equals("OPERADOR")).findFirst().orElseThrow();
        assertEquals(hashOperador, op.txHash());
        assertEquals(BASE + "/tx/" + hashOperador, op.explorerUrl());
        assertEquals("ON_CHAIN", op.modo());
        assertEquals(Instant.parse("2026-11-16T10:00:00Z"), op.firmadoEn());
    }

    @Test
    void lecturaQueLanzaExcepcionDegradaASimulado() {
        var gw = new GatewayFalso(false, null) {
            @Override public LecturaOnChain leerEstado(PropuestaAccion p) { throw new IllegalStateException("rpc caído"); }
        };
        var s = servicio(gw);
        PropuestaAccion p = s.proponer(diagnostico());
        EstadoAprobaciones e = estado(s, p);
        assertEquals("SIMULADO", e.modo());
        assertFalse(e.lecturaOnChain().esOnChain());
    }

    @Test
    void propuestaInexistenteDevuelveVacio() {
        var s = servicio(new GatewayFalso(false, null));
        assertTrue(s.aprobaciones(UUID.randomUUID()).isEmpty());
    }
}
