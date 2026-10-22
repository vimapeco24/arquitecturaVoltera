package com.voltera.liquidacion.application;

import com.voltera.liquidacion.domain.model.LiquidacionCalculada;
import com.voltera.liquidacion.domain.model.TransaccionP2P;
import com.voltera.liquidacion.domain.port.in.EmparejarOrdenesUseCase.ComandoEmparejar;
import com.voltera.liquidacion.domain.port.out.AnclajeBlockchainPort;
import com.voltera.liquidacion.domain.port.out.LiquidacionEventPublisherPort;
import com.voltera.liquidacion.domain.port.out.TransaccionRepositoryPort;
import com.voltera.liquidacion.infrastructure.blockchain.NoopAnclajeBlockchainAdapter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Verifica el cierre de la orquestación por eventos (Nivel C): al casar, el
 * servicio publica un {@link LiquidacionCalculada} con los campos que el
 * blockchain-gateway consume, incluida la comunidad (metadato off-chain).
 */
@DisplayName("Orquestación EDA - publicación de LiquidacionCalculada")
class OrquestacionEdaTest {

    @Test
    @DisplayName("Al emparejar se publica el evento con comunidad y energía en Wh")
    void publicaEventoAlCasar() {
        TransaccionRepositoryPort repo = mock(TransaccionRepositoryPort.class);
        when(repo.guardar(any(TransaccionP2P.class))).thenAnswer(inv -> inv.getArgument(0));

        List<LiquidacionCalculada> publicados = new ArrayList<>();
        LiquidacionEventPublisherPort publisher = evento -> { publicados.add(evento); return true; };

        var service = new LiquidacionService(repo, new NoopAnclajeBlockchainAdapter(), publisher);

        var comando = new ComandoEmparejar(
                "PRO-SOLAR-01", BigDecimal.valueOf(12), BigDecimal.valueOf(420),
                "PRO-CASA-07", BigDecimal.valueOf(10), BigDecimal.valueOf(500),
                "COMUNIDAD-SOLAR-01");

        service.emparejar(comando);

        assertEquals(1, publicados.size(), "debe publicarse exactamente un evento");
        LiquidacionCalculada e = publicados.get(0);
        assertNotNull(e.eventId());
        assertNotNull(e.transaccionId());
        assertEquals("PRO-SOLAR-01", e.vendedorId());
        assertEquals("PRO-CASA-07", e.compradorId());
        assertEquals("COMUNIDAD-SOLAR-01", e.comunidadId());
        // casa min(12,10)=10 kWh -> 10000 Wh
        assertEquals(10000L, e.energiaWh());
        // precio de casación = promedio(420,500) = 460
        assertEquals(0, BigDecimal.valueOf(460).compareTo(e.precioPorKwh()));
    }
}
