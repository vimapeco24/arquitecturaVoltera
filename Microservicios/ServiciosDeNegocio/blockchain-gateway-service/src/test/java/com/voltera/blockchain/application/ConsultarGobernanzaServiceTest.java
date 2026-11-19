package com.voltera.blockchain.application;

import com.voltera.blockchain.domain.model.LecturaGobernanza;
import com.voltera.blockchain.domain.port.out.ContratoGobernanzaPort;
import com.voltera.blockchain.infrastructure.blockchain.ContratoGobernanzaNoConfigurado;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ConsultarGobernanzaServiceTest {

    private static final String ID = "11111111-2222-3333-4444-555555555555";

    private static LecturaGobernanza.FirmaLeida firma(String rol, String tx) {
        return new LecturaGobernanza.FirmaLeida(rol, "0xabc", tx, Instant.parse("2026-11-16T10:00:00Z"));
    }

    /** Cliente mockeado (doble de prueba) del contrato. */
    private static class ContratoFalso implements ContratoGobernanzaPort {
        final Optional<PropuestaOnChain> propuesta;
        final List<LecturaGobernanza.FirmaLeida> firmas;
        final boolean falla;
        String idConsultado;
        ContratoFalso(Optional<PropuestaOnChain> p, List<LecturaGobernanza.FirmaLeida> f, boolean falla) {
            this.propuesta = p; this.firmas = f; this.falla = falla;
        }
        public boolean configurado() { return true; }
        public String direccionContrato() { return "0xContrato"; }
        public Optional<PropuestaOnChain> obtenerPropuesta(String id) {
            idConsultado = id;
            if (falla) throw new IllegalStateException("rpc caído");
            return propuesta;
        }
        public List<LecturaGobernanza.FirmaLeida> firmas(String id) { return firmas; }
    }

    @Test
    void sinContratoConfiguradoResponde_SIMULADO_sinFallar() {
        var s = new ConsultarGobernanzaService(new ContratoGobernanzaNoConfigurado(), "polygon-amoy");
        LecturaGobernanza l = s.leer(ID);
        assertEquals("SIMULADO", l.modo());
        assertFalse(l.existe());
        assertTrue(l.firmas().isEmpty());
        assertNotNull(l.mensaje());
    }

    @Test
    void onChain3de3Aprobada() {
        var c = new ContratoFalso(Optional.of(new ContratoGobernanzaPort.PropuestaOnChain("APROBADA", 3)),
                List.of(firma("AGENTE_IA", "0x1"), firma("OPERADOR", "0x2"), firma("PROSUMIDOR", "0x3")), false);
        LecturaGobernanza l = new ConsultarGobernanzaService(c, "polygon-amoy").leer(ID);
        assertEquals("ON_CHAIN", l.modo());
        assertTrue(l.existe());
        assertEquals("APROBADA", l.estado());
        assertEquals(3, l.aprobaciones());
        assertEquals(3, l.firmas().size());
        assertEquals("0xContrato", l.contrato());
        assertEquals(ConsultarGobernanzaService.aBytes32(ID), c.idConsultado);
        assertEquals(66, c.idConsultado.length());
    }

    @Test
    void onChain2de3Pendiente() {
        var c = new ContratoFalso(Optional.of(new ContratoGobernanzaPort.PropuestaOnChain("PENDIENTE", 2)),
                List.of(firma("AGENTE_IA", "0x1"), firma("OPERADOR", "0x2")), false);
        LecturaGobernanza l = new ConsultarGobernanzaService(c, "polygon-amoy").leer(ID);
        assertEquals("PENDIENTE", l.estado());
        assertEquals(2, l.aprobaciones());
    }

    @Test
    void onChainRechazada() {
        var c = new ContratoFalso(Optional.of(new ContratoGobernanzaPort.PropuestaOnChain("RECHAZADA", 2)),
                List.of(firma("AGENTE_IA", "0x1"), firma("OPERADOR", "0x2")), false);
        assertEquals("RECHAZADA", new ConsultarGobernanzaService(c, "polygon-amoy").leer(ID).estado());
    }

    @Test
    void propuestaInexistenteOnChain() {
        var c = new ContratoFalso(Optional.empty(), List.of(), false);
        LecturaGobernanza l = new ConsultarGobernanzaService(c, "polygon-amoy").leer(ID);
        assertEquals("ON_CHAIN", l.modo());
        assertFalse(l.existe());
    }

    @Test
    void errorDeRpcDegradaASimulado() {
        var c = new ContratoFalso(Optional.empty(), List.of(), true);
        LecturaGobernanza l = new ConsultarGobernanzaService(c, "polygon-amoy").leer(ID);
        assertEquals("SIMULADO", l.modo());
    }
}
