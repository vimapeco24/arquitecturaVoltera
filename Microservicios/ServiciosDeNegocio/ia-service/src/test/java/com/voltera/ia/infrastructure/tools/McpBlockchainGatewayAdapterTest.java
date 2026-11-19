package com.voltera.ia.infrastructure.tools;

import com.voltera.ia.domain.model.gobernanza.LecturaOnChain;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class McpBlockchainGatewayAdapterTest {

    @Test
    void parseaRespuestaOnChainDelGateway() {
        var l = McpBlockchainGatewayAdapter.parsearLectura(Map.of(
                "modo", "ON_CHAIN", "existe", true, "estado", "PENDIENTE", "aprobaciones", 2,
                "contrato", "0xC", "red", "polygon-amoy",
                "firmas", List.of(Map.of("rol", "OPERADOR", "firmante", "0xF",
                        "txHash", "0x" + "1".repeat(64), "firmadoEn", "2026-11-16T10:00:00Z"))));
        assertTrue(l.esOnChain());
        assertTrue(l.existe());
        assertEquals(2, l.aprobaciones());
        assertEquals(1, l.firmas().size());
        assertEquals(Instant.parse("2026-11-16T10:00:00Z"), l.firmas().get(0).firmadoEn());
    }

    @Test
    void respuestaSimuladaOSinModoSeTrataComoSimulada() {
        var l = McpBlockchainGatewayAdapter.parsearLectura(Map.of("modo", "SIMULADO", "mensaje", "sin contrato"));
        assertFalse(l.esOnChain());
        assertEquals("sin contrato", l.mensaje());
        assertFalse(McpBlockchainGatewayAdapter.parsearLectura(Map.of()).esOnChain());
        assertEquals(LecturaOnChain.SIMULADO, McpBlockchainGatewayAdapter.parsearLectura(Map.of("modo", "X")).modo());
    }
}
