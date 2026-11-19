package com.voltera.pagos.application;

import com.voltera.pagos.domain.model.Monto;
import com.voltera.pagos.domain.model.OrdenPago;
import com.voltera.pagos.domain.model.PagoId;
import com.voltera.pagos.domain.model.TipoPago;
import com.voltera.pagos.domain.port.in.ProcesarPagoUseCase.ComandoProcesarPago;
import com.voltera.pagos.domain.port.out.EventosPagoPort;
import com.voltera.pagos.domain.port.out.PagoRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("PagoService - publicacion de PagoConfirmado")
class PagoServiceEventosTest {

    private PagoRepositoryPort repositorio;
    private EventosPagoPort eventos;
    private PagoService service;
    private final ComandoProcesarPago comando =
            new ComandoProcesarPago("PRO-001", "FAC-100", "COBRO_FACTURA", BigDecimal.valueOf(47600));

    @BeforeEach
    void setUp() {
        repositorio = mock(PagoRepositoryPort.class);
        eventos = mock(EventosPagoPort.class);
        service = new PagoService(repositorio, eventos);
        when(repositorio.guardar(any(OrdenPago.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    @DisplayName("Procesar un pago nuevo anuncia PagoConfirmado una vez")
    void publicaAlProcesar() {
        when(repositorio.buscarPorReferencia("FAC-100")).thenReturn(Optional.empty());
        OrdenPago o = service.procesar(comando);
        verify(eventos).pagoConfirmado(o);
    }

    @Test
    @DisplayName("Idempotencia: una referencia repetida no vuelve a publicar")
    void noPublicaEnDuplicado() {
        var existente = OrdenPago.crear("PRO-001", "FAC-100", TipoPago.COBRO_FACTURA, Monto.de(47600));
        when(repositorio.buscarPorReferencia("FAC-100")).thenReturn(Optional.of(existente));
        service.procesar(comando);
        verifyNoInteractions(eventos);
    }

    @Test
    @DisplayName("Actualizar no publica un nuevo PagoConfirmado")
    void actualizarNoPublica() {
        PagoId id = PagoId.nuevo();
        when(repositorio.buscarPorId(id)).thenReturn(Optional.of(
                OrdenPago.crearCon(id, "PRO-001", "FAC-100", TipoPago.COBRO_FACTURA, Monto.de(1))));
        service.actualizar(id.valor(), comando);
        verifyNoInteractions(eventos);
    }

    @Test
    @DisplayName("Sin publicador (Kafka deshabilitado) el procesamiento funciona")
    void sinPublicador() {
        var sinEventos = new PagoService(repositorio);
        when(repositorio.buscarPorReferencia("FAC-100")).thenReturn(Optional.empty());
        assertDoesNotThrow(() -> assertEquals("PROCESADO", sinEventos.procesar(comando).estado().name()));
    }
}
