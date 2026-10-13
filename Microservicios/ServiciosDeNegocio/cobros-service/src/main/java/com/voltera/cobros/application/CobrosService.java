package com.voltera.cobros.application;

import com.voltera.cobros.domain.event.PagoConfirmado;
import com.voltera.cobros.domain.exception.MovimientoNoEncontradoException;
import com.voltera.cobros.domain.model.MensajeInbox;
import com.voltera.cobros.domain.model.Movimiento;
import com.voltera.cobros.domain.port.in.ConsultarMovimientoUseCase;
import com.voltera.cobros.domain.port.in.ProcesarPagoConfirmadoUseCase;
import com.voltera.cobros.domain.port.in.RegistrarMovimientoUseCase;
import com.voltera.cobros.domain.port.out.InboxPort;
import com.voltera.cobros.domain.port.out.MovimientoRepositoryPort;

import java.util.List;

/**
 * Servicio de aplicación del BC <b>Cobros</b> (lámina 02).
 *
 * <p>Registra movimientos de cobro de prima y reacciona al evento
 * {@code PagoConfirmado} para conciliarlos (emitir recibo). El procesamiento del
 * evento es idempotente mediante una tabla INBOX por consumidor: una reentrega del
 * mismo {@code eventId} no concilia ni emite el recibo dos veces. Los datos del pago
 * viajan en el evento (transferencia de estado); no se consulta al servicio de Pagos.</p>
 */
public class CobrosService implements RegistrarMovimientoUseCase, ProcesarPagoConfirmadoUseCase,
        ConsultarMovimientoUseCase {

    /** Nombre lógico del consumer group (clave de la inbox). */
    private static final String CONSUMIDOR = "cobros-consumer";

    private final MovimientoRepositoryPort repositorio;
    private final InboxPort inbox;

    public CobrosService(MovimientoRepositoryPort repositorio, InboxPort inbox) {
        this.repositorio = repositorio;
        this.inbox = inbox;
    }

    @Override
    public Movimiento registrar(ComandoRegistrarMovimiento c) {
        Movimiento movimiento = Movimiento.registrar(c.clienteId(), c.polizaId(), c.prima());
        return repositorio.guardar(movimiento);
    }

    @Override
    public Movimiento procesar(PagoConfirmado evento) {
        // 1. IDEMPOTENCIA (tabla INBOX por consumidor): descarta reentregas.
        boolean esNuevo = inbox.registrarSiEsNuevo(CONSUMIDOR, evento.eventId(), "PagoConfirmado");
        if (!esNuevo) {
            return repositorio.buscarPorPoliza(evento.polizaId()).stream()
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException(
                            "Evento duplicado en inbox pero sin movimiento asociado: " + evento.eventId()));
        }

        // 2. Localiza el movimiento PENDIENTE de la póliza; si no existe, lo crea
        //    con los datos del evento (transferencia de estado).
        Movimiento movimiento = repositorio.buscarPendientePorPoliza(evento.polizaId())
                .orElseGet(() -> Movimiento.registrar(
                        evento.clienteId(),
                        evento.polizaId(),
                        com.voltera.cobros.domain.model.Prima.de(
                                evento.montoPagado(),
                                com.voltera.cobros.domain.model.Prima.Periodicidad.MENSUAL)));

        // 3. Concilia el movimiento con el pago (emite recibo, pasa a CONCILIADO).
        movimiento.conciliarConPago(evento.referencia(), evento.montoPagado());
        return repositorio.guardar(movimiento);
    }

    @Override
    public Movimiento porId(String id) {
        return repositorio.buscarPorId(id)
                .orElseThrow(() -> new MovimientoNoEncontradoException("No se encontró el movimiento: " + id));
    }

    @Override
    public List<Movimiento> porPoliza(String polizaId) {
        return repositorio.buscarPorPoliza(polizaId);
    }

    @Override
    public List<Movimiento> todos() {
        return repositorio.buscarTodos();
    }

    public long inboxUnicos() { return inbox.totalUnicos(); }
    public long inboxDuplicados() { return inbox.totalDuplicados(); }
    public List<MensajeInbox> inboxFilas() { return inbox.todos(); }
}
