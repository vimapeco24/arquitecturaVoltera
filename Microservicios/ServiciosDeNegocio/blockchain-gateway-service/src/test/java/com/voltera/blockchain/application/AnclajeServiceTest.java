package com.voltera.blockchain.application;

import com.voltera.blockchain.domain.model.Anclaje;
import com.voltera.blockchain.domain.model.EstadoAnclaje;
import com.voltera.blockchain.domain.model.LiquidacionAnclada;
import com.voltera.blockchain.domain.model.LiquidacionP2P;
import com.voltera.blockchain.domain.port.out.CadenaBlockchainPort;
import com.voltera.blockchain.domain.port.out.EventPublisherPort;
import com.voltera.blockchain.infrastructure.crypto.Web3jHashService;
import com.voltera.blockchain.infrastructure.persistence.InMemoryAnclajeRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AnclajeServiceTest {

    private LiquidacionP2P liq(String id) {
        return new LiquidacionP2P(id, "prosumidor-A", "prosumidor-B",
                12_500L, new BigDecimal("0.45"), new BigDecimal("5.625"),
                Instant.parse("2026-10-20T10:00:00Z"));
    }

    /** Cadena que confirma siempre; registra lo que se le pidio anclar. */
    private static class CadenaOk implements CadenaBlockchainPort {
        int llamadas = 0;
        public ResultadoAnclaje anclar(Anclaje a) { llamadas++; return ResultadoAnclaje.ok("0xTX" + a.transaccionId()); }
        public boolean disponible() { return true; }
    }

    /** Cadena que falla transitoriamente (deja PENDIENTE). */
    private static class CadenaCaida implements CadenaBlockchainPort {
        public ResultadoAnclaje anclar(Anclaje a) { return ResultadoAnclaje.transitorio("rpc down"); }
        public boolean disponible() { return false; }
    }

    private static class PublicadorSpy implements EventPublisherPort {
        final List<LiquidacionAnclada> publicados = new ArrayList<>();
        public boolean publicar(LiquidacionAnclada e) { publicados.add(e); return true; }
    }

    @Test
    void ancla_confirma_y_publica_evento() {
        var repo = new InMemoryAnclajeRepository();
        var cadena = new CadenaOk();
        var pub = new PublicadorSpy();
        var svc = new AnclajeService(repo, cadena, pub, new Web3jHashService(), "polygon-amoy");

        Anclaje a = svc.anclar(liq("TX-1"));

        assertEquals(EstadoAnclaje.CONFIRMADO, a.estado());
        assertNotNull(a.txHash());
        assertTrue(a.hashCasacion().startsWith("0x"));
        assertEquals(1, pub.publicados.size());
        assertEquals("TX-1", pub.publicados.get(0).transaccionId());
    }

    @Test
    void anclar_es_idempotente_por_transaccion() {
        var repo = new InMemoryAnclajeRepository();
        var cadena = new CadenaOk();
        var svc = new AnclajeService(repo, cadena, new PublicadorSpy(), new Web3jHashService(), "polygon-amoy");

        svc.anclar(liq("TX-DUP"));
        svc.anclar(liq("TX-DUP"));

        assertEquals(1, repo.buscarTodos().size());
    }

    @Test
    void si_la_cadena_esta_caida_queda_pendiente_y_el_relay_reintenta() {
        var repo = new InMemoryAnclajeRepository();
        var caida = new CadenaCaida();
        var pub = new PublicadorSpy();
        var svc = new AnclajeService(repo, caida, pub, new Web3jHashService(), "polygon-amoy");

        Anclaje a = svc.anclar(liq("TX-2"));
        assertEquals(EstadoAnclaje.PENDIENTE, a.estado());
        assertTrue(pub.publicados.isEmpty());
        assertEquals(1, repo.buscarPendientes().size());

        // El relay con la cadena caida no rompe nada.
        svc.procesarPendientes();
        assertEquals(1, repo.buscarPendientes().size());
    }

    @Test
    void el_relay_confirma_cuando_la_cadena_vuelve() {
        var repo = new InMemoryAnclajeRepository();
        var caida = new CadenaCaida();
        var pub = new PublicadorSpy();
        var svc = new AnclajeService(repo, caida, pub, new Web3jHashService(), "polygon-amoy");
        svc.anclar(liq("TX-3"));
        assertEquals(1, repo.buscarPendientes().size());

        // Se "repara" la cadena: nuevo servicio con cadena OK sobre el mismo repo.
        var cadenaOk = new CadenaOk();
        var svc2 = new AnclajeService(repo, cadenaOk, pub, new Web3jHashService(), "polygon-amoy");
        svc2.procesarPendientes();

        assertTrue(repo.buscarPendientes().isEmpty());
        assertEquals(1, pub.publicados.size());
    }

    @Test
    void el_hash_canonico_es_estable() {
        var hs = new Web3jHashService();
        String h1 = hs.keccak256Hex(liq("TX-X"));
        String h2 = hs.keccak256Hex(liq("TX-X"));
        assertEquals(h1, h2);
        assertEquals(66, h1.length()); // 0x + 64 hex
    }

    @Test
    void verificar_confirma_integridad_y_detecta_alteracion() {
        var repo = new InMemoryAnclajeRepository();
        var svc = new AnclajeService(repo, new CadenaOk(), new PublicadorSpy(),
                new Web3jHashService(), "polygon-amoy");
        svc.anclar(liq("TX-V"));

        // Mismo contenido -> integro
        var ok = svc.verificar(liq("TX-V"));
        assertTrue(ok.existe());
        assertTrue(ok.integro());
        assertEquals(ok.hashRecomputado(), ok.hashAnclado());

        // Contenido alterado (otra energia) -> NO integro
        var alterada = new LiquidacionP2P("TX-V", "prosumidor-A", "prosumidor-B",
                99_999L, new java.math.BigDecimal("0.45"), new java.math.BigDecimal("5.625"),
                java.time.Instant.parse("2026-10-20T10:00:00Z"));
        var malo = svc.verificar(alterada);
        assertTrue(malo.existe());
        assertFalse(malo.integro());

        // Transaccion inexistente -> SIN_ANCLAR
        var inexistente = svc.verificar(liq("TX-NO"));
        assertFalse(inexistente.existe());
        assertEquals("SIN_ANCLAR", inexistente.estado());
    }
}
