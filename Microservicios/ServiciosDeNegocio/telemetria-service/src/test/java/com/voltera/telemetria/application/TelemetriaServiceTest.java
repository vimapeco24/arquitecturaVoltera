package com.voltera.telemetria.application;

import com.voltera.telemetria.domain.model.ConsumoRegistrado;
import com.voltera.telemetria.domain.model.EstadoMedidor;
import com.voltera.telemetria.domain.model.Medidor;
import com.voltera.telemetria.domain.port.in.IngestarConsumoUseCase.ComandoIngestarConsumo;
import com.voltera.telemetria.domain.port.in.GestionarMedidorUseCase.ComandoRegistrarMedidor;
import com.voltera.telemetria.domain.port.out.EventPublisherPort;
import com.voltera.telemetria.infrastructure.persistence.InMemoryBufferOffline;
import com.voltera.telemetria.infrastructure.persistence.InMemoryMedidorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("TelemetriaService - ingesta IoT, consumo extra y store-and-forward")
class TelemetriaServiceTest {

    private InMemoryMedidorRepository repositorio;
    private InMemoryBufferOffline buffer;
    private PublisherControlable publisher;
    private TelemetriaService service;

    /** Publisher de prueba: se puede "desconectar" para simular la caida del broker. */
    static class PublisherControlable implements EventPublisherPort {
        boolean conectado = true;
        final List<ConsumoRegistrado> publicados = new ArrayList<>();

        @Override
        public boolean publicar(ConsumoRegistrado evento) {
            if (!conectado) {
                return false;
            }
            publicados.add(evento);
            return true;
        }
    }

    @BeforeEach
    void setUp() {
        repositorio = new InMemoryMedidorRepository();
        buffer = new InMemoryBufferOffline();
        publisher = new PublisherControlable();
        service = new TelemetriaService(repositorio, publisher, buffer);
    }

    private Medidor medidorActivo(double umbral) {
        Medidor m = service.registrar(new ComandoRegistrarMedidor("PRO-1", BigDecimal.valueOf(umbral)));
        return service.activar(m.id().valor());
    }

    @Test
    @DisplayName("Registrar un medidor lo deja INACTIVO; activarlo lo pasa a ACTIVO")
    void registrarYActivar() {
        Medidor m = service.registrar(new ComandoRegistrarMedidor("PRO-1", BigDecimal.valueOf(100)));
        assertEquals(EstadoMedidor.INACTIVO, m.estado());

        Medidor activo = service.activar(m.id().valor());
        assertEquals(EstadoMedidor.ACTIVO, activo.estado());
    }

    @Test
    @DisplayName("Ingesta con broker disponible publica el evento y NO usa el buffer")
    void ingestaPublicaAlBroker() {
        Medidor m = medidorActivo(100);

        var r = service.ingestar(new ComandoIngestarConsumo(m.id().valor(), BigDecimal.valueOf(50), Instant.now()));

        assertTrue(r.publicadoEnBroker());
        assertFalse(r.enBufferOffline());
        assertEquals(1, publisher.publicados.size());
        assertEquals(0, buffer.tamano());
        assertFalse(r.evento().consumoExtra()); // 50 <= 100
    }

    @Test
    @DisplayName("Una lectura por encima del umbral se marca como consumo extra")
    void consumoExtraSobreUmbral() {
        Medidor m = medidorActivo(100);

        var r = service.ingestar(new ComandoIngestarConsumo(m.id().valor(), BigDecimal.valueOf(150), Instant.now()));

        assertTrue(r.evento().consumoExtra()); // 150 > 100
    }

    @Test
    @DisplayName("Solo un medidor ACTIVO puede registrar consumo")
    void soloMedidorActivoMide() {
        Medidor m = service.registrar(new ComandoRegistrarMedidor("PRO-1", BigDecimal.valueOf(100)));
        assertThrows(IllegalStateException.class,
                () -> service.ingestar(new ComandoIngestarConsumo(m.id().valor(), BigDecimal.valueOf(10), Instant.now())));
    }

    @Test
    @DisplayName("Store-and-forward: broker caido encola en buffer; al reconectar drena y reenvia sin perder eventos")
    void storeAndForward() {
        Medidor m = medidorActivo(100);

        // 1) Broker caido: 3 lecturas -> se van al buffer offline (procesar sin conexion).
        publisher.conectado = false;
        for (int i = 0; i < 3; i++) {
            var r = service.ingestar(new ComandoIngestarConsumo(m.id().valor(), BigDecimal.valueOf(10 + i), Instant.now()));
            assertFalse(r.publicadoEnBroker());
            assertTrue(r.enBufferOffline());
        }
        assertEquals(3, buffer.tamano());
        assertEquals(0, publisher.publicados.size());

        // 2) Se recupera la conexion y se drena el buffer (desencolar).
        publisher.conectado = true;
        var drenado = service.drenar();

        assertEquals(3, drenado.reenviados());
        assertEquals(0, drenado.pendientes());
        assertEquals(0, buffer.tamano());
        assertEquals(3, publisher.publicados.size()); // ningun evento se perdio
    }
}
